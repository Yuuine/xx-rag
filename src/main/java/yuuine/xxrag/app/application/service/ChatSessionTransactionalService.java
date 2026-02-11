package yuuine.xxrag.app.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yuuine.xxrag.app.domain.model.ChatHistory;
import yuuine.xxrag.app.domain.model.ChatSession;
import yuuine.xxrag.app.domain.repository.ChatHistoryMapper;
import yuuine.xxrag.app.domain.repository.ChatSessionMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 封装需要事务保护的多步数据库操作
 */
@Service
@Slf4j
public class ChatSessionTransactionalService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatSessionTransactionalService self;

    public ChatSessionTransactionalService(ChatSessionMapper chatSessionMapper, ChatHistoryMapper chatHistoryMapper, ChatSessionTransactionalService self) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatHistoryMapper = chatHistoryMapper;
        this.self = self;
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistSessionAndHistories(String sessionId, LocalDateTime createdAt, LocalDateTime lastAccessTime, List<ChatHistory> messages) {
        ChatSession dbSession = new ChatSession(sessionId, createdAt, lastAccessTime);
        chatSessionMapper.saveOrUpdateSession(dbSession);
        for (ChatHistory m : messages) {
            m.setSessionId(sessionId);
            int result = chatHistoryMapper.save(m);
            log.debug("保存{}条消息", result);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSessionAndHistories(String sessionId) {
        int result = chatHistoryMapper.deleteBySessionId(sessionId);
        chatSessionMapper.deleteSession(sessionId);
        log.debug("已删除会话{}的{}条历史记录", sessionId, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBySessionIdAndDate(String sessionId, LocalDateTime beforeDate) {
        int result = chatHistoryMapper.deleteBySessionIdAndDate(sessionId, beforeDate);
        log.debug("已删除会话{}在{}之前创建的{}条历史记录", sessionId, beforeDate, result);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAllSessionsTransactional() {
        int result1 = chatHistoryMapper.deleteAll();
        int result2 = chatSessionMapper.deleteAll();
        log.debug("已删除{}条历史记录和{}个会话", result1, result2);
    }

    /**
     * 查找超过指定时间未更新的会话
     */
    public List<String> findInactiveSessions(LocalDateTime cutoffDate) {
        return chatSessionMapper.findInactiveSessions(cutoffDate);
    }

    /**
     * 根据会话ID列表批量删除会话及历史记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteSessions(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        for (String sessionId : sessionIds) {
            try {
                deleteSessionAndHistories(sessionId);
                log.debug("已删除过期会话：{}", sessionId);
            } catch (Exception e) {
                log.error("删除过期会话 {} 时发生错误", sessionId, e);
            }
        }
    }

    /**
     * 异步批量删除会话
     */
    public void batchDeleteSessionsAsync(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        // 使用并行流异步处理删除操作
        sessionIds.parallelStream().forEach(sessionId -> {
            try {
                self.deleteSessionAndHistories(sessionId);
                log.debug("已异步删除过期会话：{}", sessionId);
            } catch (Exception e) {
                log.error("异步删除过期会话 {} 时发生错误", sessionId, e);
            }
        });
    }
}
