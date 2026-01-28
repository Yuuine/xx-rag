package yuuine.xxrag.app.application.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.domain.model.ChatHistory;
import yuuine.xxrag.app.domain.model.ChatSession;
import yuuine.xxrag.app.domain.repository.ChatHistoryMapper;
import yuuine.xxrag.app.domain.repository.ChatSessionMapper;
import yuuine.xxrag.dto.request.InferenceRequest;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 Cookie + Session 的对话历史管理服务
 * 使用内存缓存累积对话记录，定期批量持久化到数据库
 */
@Service
@Slf4j
@NamedInterface("chatSessionService")
public class ChatSessionService {

    @Value("${app.chat.history.flush-threshold:10}")
    private int flushThreshold;

    @Value("${app.chat.session.expiry-minutes:30}")
    private int sessionExpiryMinutes;

    @Data
    public static class SessionCache {
        private final String sessionId;
        private final List<ChatHistory> pendingMessages = new ArrayList<>();
        private LocalDateTime lastAccessTime;
        private final LocalDateTime createdAt;

        public SessionCache(String sessionId) {
            this.sessionId = sessionId;
            this.lastAccessTime = LocalDateTime.now();
            this.createdAt = LocalDateTime.now();
        }

        public void addPendingMessage(ChatHistory message) {
            pendingMessages.add(message);
            lastAccessTime = LocalDateTime.now();
        }

        public boolean shouldFlush(int threshold) {
            return pendingMessages.size() >= threshold;
        }

        public boolean isExpired(int expiryMinutes) {
            return lastAccessTime.isBefore(LocalDateTime.now().minusMinutes(expiryMinutes));
        }
    }

    private final Map<String, SessionCache> sessionCache = new ConcurrentHashMap<>();

    private final ChatSessionMapper chatSessionMapper;
    private final ChatHistoryMapper chatHistoryMapper;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public ChatSessionService(ChatSessionMapper chatSessionMapper, ChatHistoryMapper chatHistoryMapper) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatHistoryMapper = chatHistoryMapper;
    }

    // 在构造完成后启动定时任务
    {
        scheduler.scheduleAtFixedRate(this::flushPendingSessions, 30, 30, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }

    public SessionCache getSession(String sessionId) {
        return sessionCache.computeIfAbsent(sessionId, SessionCache::new);
    }

    public void addMessageToSession(String sessionId, String role, String content) {
        SessionCache session = getSession(sessionId);
        ChatHistory message = new ChatHistory(null, sessionId, role, content, LocalDateTime.now());

        synchronized (this) {
            session.addPendingMessage(message);
        }

        log.debug("添加消息到会话 {} (缓存)，角色: {}, 内容长度: {}", sessionId, role, content.length());

        if (session.shouldFlush(flushThreshold)) {
            flushSession(sessionId);
        }
    }

    public void addUserMessage(String sessionId, String content) {
        addMessageToSession(sessionId, "user", content);
    }

    public void addAssistantMessage(String sessionId, String content) {
        addMessageToSession(sessionId, "assistant", content);
    }

    public void addSystemMessage(String sessionId, String content) {
        addMessageToSession(sessionId, "system", content);
    }

    public synchronized void flushSession(String sessionId) {
        SessionCache session = sessionCache.get(sessionId);
        if (session == null || session.getPendingMessages().isEmpty()) {
            return;
        }

        try {
            ChatSession dbSession = new ChatSession(sessionId, session.getCreatedAt(), session.getLastAccessTime());
            chatSessionMapper.saveOrUpdateSession(dbSession);

            for (ChatHistory message : session.getPendingMessages()) {
                chatHistoryMapper.save(message);
            }

            log.debug("会话 {} 刷新 {} 条消息到数据库完成", sessionId, session.getPendingMessages().size());

            session.getPendingMessages().clear();

        } catch (Exception e) {
            log.error("会话 {} 刷新到数据库失败，消息保留在内存中", sessionId, e);
        }
    }

    public void flushAllPendingSessions() {
        for (String sessionId : sessionCache.keySet()) {
            flushSession(sessionId);
        }
    }

    private void flushPendingSessions() {
        log.debug("开始定期刷新会话缓存");
        int count = 0;

        for (String sessionId : sessionCache.keySet()) {
            try {
                flushSession(sessionId);
                count++;
            } catch (Exception e) {
                log.error("定期刷新会话 {} 失败", sessionId, e);
            }
        }

        if (count > 0) {
            log.debug("本次定期刷新处理了 {} 个会话", count);
        }
    }

    public List<InferenceRequest.Message> getSessionHistory(String sessionId) {
        List<InferenceRequest.Message> messages = new ArrayList<>();

        // 数据库部分
        List<ChatHistory> dbMessages = chatHistoryMapper.findBySessionId(sessionId);
        for (ChatHistory msg : dbMessages) {
            messages.add(new InferenceRequest.Message(msg.getRole(), msg.getContent()));
        }

        // 缓存部分同步读取，避免并发修改时的结构不一致
        SessionCache session = sessionCache.get(sessionId);
        if (session != null) {
            synchronized (this) {
                for (ChatHistory cached : session.getPendingMessages()) {
                    messages.add(new InferenceRequest.Message(cached.getRole(), cached.getContent()));
                }
            }
        }

        return messages;
    }

    public void clearSessionCache(String sessionId) {
        SessionCache session = sessionCache.get(sessionId);
        if (session != null) {
            synchronized (this) {
                session.getPendingMessages().clear();
            }
            log.debug("已清除会话 {} 的内存待刷消息", sessionId);
        }
    }

    public void deleteSession(String sessionId) {
        // 清空内存（加锁保护）
        synchronized (this) {
            SessionCache session = sessionCache.get(sessionId);
            if (session != null) {
                session.getPendingMessages().clear();
            }
        }

        // 删除数据库记录
        chatHistoryMapper.deleteBySessionId(sessionId);
        chatSessionMapper.deleteSession(sessionId);

        sessionCache.remove(sessionId);

        log.debug("会话 {} 及其所有历史已删除", sessionId);
    }

    private void cleanupExpiredSessions() {
        log.debug("开始清理过期会话缓存");
        AtomicInteger cleaned = new AtomicInteger(0);

        sessionCache.entrySet().removeIf(entry -> {
            SessionCache session = entry.getValue();
            if (!session.isExpired(sessionExpiryMinutes)) {
                return false;
            }

            String sessionId = entry.getKey();
            try {
                flushSession(sessionId);
                cleaned.incrementAndGet();
                log.debug("过期会话 {} 已持久化并移除", sessionId);
                return true;
            } catch (Exception e) {
                log.warn("过期会话 {} 持久化失败，本次不移除以防数据丢失", sessionId, e);
                return false;  // 失败 → 不移除
            }
        });

        if (cleaned.get() > 0) {
            log.info("本次过期会话清理移除 {} 个会话", cleaned.get());
        }
    }

    @PreDestroy
    public void onDestroy() {
        log.info("ChatSessionService 正在关闭，开始最终持久化");

        flushAllPendingSessions();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(25, TimeUnit.SECONDS)) {
                log.warn("定时任务未在 25 秒内完成，强制关闭");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("等待定时任务关闭被中断", e);
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("ChatSessionService 已关闭");
    }
}