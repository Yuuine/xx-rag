package yuuine.xxrag.app.domain.repository;

import org.apache.ibatis.annotations.*;
import yuuine.xxrag.app.domain.model.ChatHistory;

import java.time.LocalDateTime;
import java.util.List;

// ChatHistoryMapper.java
@Mapper
public interface ChatHistoryMapper {
    int save(ChatHistory chatHistory);

    List<ChatHistory> findBySessionId(@Param("sessionId") String sessionId);

    int deleteBySessionId(@Param("sessionId") String sessionId);

    int deleteBySessionIdAndDate(@Param("sessionId") String sessionId,
                                 @Param("beforeDate") LocalDateTime beforeDate);

    // 删除所有会话中在 beforeDate 之前的消息
    int deleteByDate(@Param("beforeDate") LocalDateTime beforeDate);

    // 删除所有历史记录（危险）
    int deleteAll();
}