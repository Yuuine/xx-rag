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

    public ChatSessionTransactionalService(ChatSessionMapper chatSessionMapper, ChatHistoryMapper chatHistoryMapper) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatHistoryMapper = chatHistoryMapper;
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
}
