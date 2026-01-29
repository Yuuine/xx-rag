package yuuine.xxrag.app.application.service;

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
            // ensure sessionId is correct
            m.setSessionId(sessionId);
            chatHistoryMapper.save(m);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSessionAndHistories(String sessionId) {
        chatHistoryMapper.deleteBySessionId(sessionId);
        chatSessionMapper.deleteSession(sessionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBySessionIdAndDate(String sessionId, LocalDateTime beforeDate) {
        chatHistoryMapper.deleteBySessionIdAndDate(sessionId, beforeDate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAllSessionsTransactional() {
        chatHistoryMapper.deleteAll();
        chatSessionMapper.deleteAll();
    }
}
