package yuuine.xxrag.app.domain.repository;

import org.apache.ibatis.annotations.Mapper;
import yuuine.xxrag.app.domain.model.ChatSession;


@Mapper
public interface ChatSessionMapper {
    void saveOrUpdateSession(ChatSession session);

    void deleteSession(String sessionId);

    int deleteAll();
}