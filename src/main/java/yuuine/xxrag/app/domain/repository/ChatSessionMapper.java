package yuuine.xxrag.app.domain.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import yuuine.xxrag.app.domain.model.ChatSession;

import java.time.LocalDateTime;
import java.util.List;


@Mapper
public interface ChatSessionMapper {
    void saveOrUpdateSession(ChatSession session);

    void deleteSession(String sessionId);

    int deleteAll();
    /**
     *
     * 查找超过指定时间未更新的会话ID列表
     */
    List<String> findInactiveSessions(@Param("cutoffDate") LocalDateTime cutoffDate);
}