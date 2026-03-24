package yuuine.xxrag.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class RagWebSocketSessionManager {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<>();

    public void register(Session session) {
        sessions.add(session);
        log.info("WebSocket 连接已注册，当前连接数: {}", sessions.size());
    }

    public void unregister(Session session) {
        sessions.remove(session);
        log.info("WebSocket 连接已注销，当前连接数: {}", sessions.size());
    }

    public void sendToSession(Session session, Object message) {
        try {
            if (session != null && session.isOpen()) {
                String jsonMessage;
                if (message instanceof String) {
                    jsonMessage = (String) message;
                } else {
                    jsonMessage = OBJECT_MAPPER.writeValueAsString(message);
                }
                session.getBasicRemote().sendText(jsonMessage);
            }
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }

    public void sendMessageToSession(String sessionId, Object message) {
        log.warn("sendMessageToSession 不再支持，请使用 sendToSession 或 broadcast");
    }

    public void broadcast(Object message) {
        for (Session session : sessions) {
            try {
                sendToSession(session, message);
            } catch (Exception e) {
                log.error("广播消息失败", e);
            }
        }
    }

    public int connectionCount() {
        return sessions.size();
    }
}
