package yuuine.xxrag.app.domain.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import yuuine.xxrag.app.domain.model.ChatSession;

import java.time.LocalDateTime;

@Mapper
public interface ChatSessionMapper {
    void saveOrUpdateSession(ChatSession session);

    ChatSession findBySessionId(String sessionId);

    void updateSessionUpdatedTime(@Param("sessionId") String sessionId,
                                  @Param("updatedAt") LocalDateTime updatedAt);

    void deleteSession(String sessionId);

    // 删除所有会话记录
    void deleteAll();
}