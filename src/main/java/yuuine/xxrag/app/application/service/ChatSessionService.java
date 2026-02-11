package yuuine.xxrag.app.application.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.config.ChatHistoryProperties;
import yuuine.xxrag.app.domain.model.ChatHistory;
import yuuine.xxrag.app.domain.repository.ChatHistoryMapper;
import yuuine.xxrag.dto.request.InferenceRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 Cookie + Session 的对话历史管理服务
 * 使用内存缓存累积对话记录，定期批量持久化到数据库
 */
@Service
@Slf4j
@NamedInterface("chatSessionService")
public class ChatSessionService {

    private int flushThreshold = 10;

    private int sessionExpiryMinutes = 30;

    @Data
    public static class SessionCache {
        private final String sessionId;
        private final List<ChatHistory> pendingMessages = new CopyOnWriteArrayList<>();
        private volatile LocalDateTime lastAccessTime;
        private final LocalDateTime createdAt;
        private final Object lock = new Object();  // 每个会话独立锁

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

        public void updateLastAccessTime() {
            lastAccessTime = LocalDateTime.now();
        }
    }

    private final Map<String, SessionCache> sessionCache = new ConcurrentHashMap<>();

    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatHistoryProperties chatHistoryProperties;
    private final ChatSessionTransactionalService chatSessionTransactionalService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public ChatSessionService(ChatHistoryMapper chatHistoryMapper, ChatHistoryProperties chatHistoryProperties, ChatSessionTransactionalService chatSessionTransactionalService) {
        this.chatHistoryMapper = chatHistoryMapper;
        this.chatHistoryProperties = chatHistoryProperties;
        this.chatSessionTransactionalService = chatSessionTransactionalService;
    }

    @PostConstruct
    public void init() {
        if (chatHistoryProperties != null) {
            flushThreshold = chatHistoryProperties.getFlushThreshold();
            sessionExpiryMinutes = chatHistoryProperties.getSessionExpiryMinutes();
        }
        scheduler.scheduleAtFixedRate(this::flushPendingSessions, 60, 120, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null) return null;
        return sessionId.replace("-", "").toLowerCase();
    }

    public SessionCache getSession(String sessionId) {
        if (sessionId == null) return null;
        String sid = normalizeSessionId(sessionId);
        return sessionCache.computeIfAbsent(sid, SessionCache::new);
    }

    public void addMessageToSession(String sessionId, String role, String content) {
        if (sessionId == null) {
            log.warn("尝试向 null 会话添加消息，忽略操作");
            return;
        }

        String sid = normalizeSessionId(sessionId);
        SessionCache session = getSession(sid);
        ChatHistory message = new ChatHistory(null, sid, role, content, LocalDateTime.now());

        synchronized (session.getLock()) {
            session.addPendingMessage(message);
        }

        log.debug("添加消息到会话 {} (缓存)，角色: {}, 内容长度: {}", sid, role, content.length());

        if (session.shouldFlush(flushThreshold)) {
            flushSession(sid);
        }
    }

    public void addUserMessage(String sessionId, String content) {
        addMessageToSession(sessionId, "user", content);
    }

    public void addAssistantMessage(String sessionId, String content) {
        addMessageToSession(sessionId, "assistant", content);
    }

    public void flushSession(String sessionId) {
        if (sessionId == null) return;

        String sid = normalizeSessionId(sessionId);

        SessionCache session = sessionCache.get(sid);
        if (session == null) {
            return;
        }

        List<ChatHistory> messagesToFlush;
        synchronized (session.getLock()) {
            if (session.getPendingMessages().isEmpty()) {
                return;
            }
            messagesToFlush = new ArrayList<>(session.getPendingMessages());
            session.getPendingMessages().clear();
        }

        try {
            // use transactional service to persist session and messages atomically
            chatSessionTransactionalService.persistSessionAndHistories(sid, session.getCreatedAt(), session.getLastAccessTime(), messagesToFlush);

            log.debug("会话 {} 刷新 {} 条消息到数据库完成", sid, messagesToFlush.size());

        } catch (Exception e) {
            // 失败时回滚清空操作
            synchronized (session.getLock()) {
                session.getPendingMessages().addAll(messagesToFlush);
            }
            log.error("会话 {} 刷新到数据库失败，消息已恢复到缓存", sid, e);
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
        int maxHistoryMessages = chatHistoryProperties.getMaxHistoryMessages();
        return getSessionHistory(sessionId, maxHistoryMessages);
    }

    public List<InferenceRequest.Message> getSessionHistory(String sessionId, int maxMessages) {
        if (sessionId == null) return new ArrayList<>();

        String sid = normalizeSessionId(sessionId);

        SessionCache session = getSession(sid);
        if (session != null) session.updateLastAccessTime();  // 读取历史时更新访问时间

        List<InferenceRequest.Message> messages = new ArrayList<>();

        // 从数据库获取最新限制数量的历史记录
        List<ChatHistory> dbMessages = chatHistoryMapper.findBySessionIdWithLimit(sid, maxMessages);

        // 反转恢复时间顺序
        for (int i = dbMessages.size() - 1; i >= 0; i--) {
            ChatHistory msg = dbMessages.get(i);
            messages.add(new InferenceRequest.Message(msg.getRole(), msg.getContent()));
        }

        // 缓存部分（无锁读取，CopyOnWriteArrayList 支持高效并发读）
        List<ChatHistory> cachedMessages = session != null ? session.getPendingMessages() : new ArrayList<>();

        if (cachedMessages.size() + messages.size() <= maxMessages) {
            for (ChatHistory cached : cachedMessages) {
                messages.add(new InferenceRequest.Message(cached.getRole(), cached.getContent()));
            }
        } else {
            int remainingSpace = maxMessages - messages.size();
            if (remainingSpace > 0) {
                int startIndex = Math.max(0, cachedMessages.size() - remainingSpace);
                for (int i = startIndex; i < cachedMessages.size(); i++) {
                    messages.add(new InferenceRequest.Message(cachedMessages.get(i).getRole(), cachedMessages.get(i).getContent()));
                }
            }
        }

        return messages;
    }

    public int getMaxHistoryEchoMessages() {
        return chatHistoryProperties != null ? chatHistoryProperties.getMaxHistoryEchoMessages() : 20;
    }

    public void clearSessionCache(String sessionId) {
        if (sessionId == null) return;

        String sid = normalizeSessionId(sessionId);

        SessionCache session = sessionCache.get(sid);
        if (session != null) {
            synchronized (session.getLock()) {
                session.getPendingMessages().clear();
            }
            log.debug("已清除会话 {} 的内存待刷消息", sid);
        }
    }

    public void deleteSessionBefore(String sessionId, LocalDateTime beforeDate) {
        if (sessionId == null || beforeDate == null) return;

        String sid = normalizeSessionId(sessionId);

        SessionCache session = sessionCache.get(sid);
        if (session != null) {
            synchronized (session.getLock()) {
                session.getPendingMessages().removeIf(msg -> msg.getCreatedAt().isBefore(beforeDate));
            }
            session.updateLastAccessTime();  // 更新访问时间
        }

        try {
            // delegate to transactional service for delete by date
            chatSessionTransactionalService.deleteBySessionIdAndDate(sid, beforeDate);
            log.debug("已删除会话 {} 中 {} 之前的历史记录", sid, beforeDate);
        } catch (Exception e) {
            log.error("删除会话 {} 的历史时出错", sid, e);
            throw e;
        }
    }

    public void deleteSession(String sessionId) {
        if (sessionId == null) return;

        String sid = normalizeSessionId(sessionId);

        clearSessionCache(sid);
        // delete histories and session in a single transaction
        chatSessionTransactionalService.deleteSessionAndHistories(sid);
        sessionCache.remove(sid);
        log.debug("会话 {} 及其所有历史已删除", sid);
    }

    public void deleteAllSessions() {
        sessionCache.clear();
        try {
            chatSessionTransactionalService.deleteAllSessionsTransactional();
            log.warn("已删除所有会话及其历史（危险操作）");
        } catch (Exception e) {
            log.error("删除所有会话失败", e);
            throw e;
        }
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
                return false;
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
