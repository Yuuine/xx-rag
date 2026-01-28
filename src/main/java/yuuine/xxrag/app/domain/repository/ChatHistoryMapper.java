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
}