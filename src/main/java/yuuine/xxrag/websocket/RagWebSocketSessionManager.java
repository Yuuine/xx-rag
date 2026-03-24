package yuuine.xxrag.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class RagWebSocketSessionManager {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CopyOnWriteArraySet<RagWebSocketHandler> webSocketSet = new CopyOnWriteArraySet<>();
    private final ConcurrentHashMap<String, RagWebSocketHandler> sessionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> webSocketToBusinessSessionMap = new ConcurrentHashMap<>();

    public void register(Session session, RagWebSocketHandler handler, String businessSessionId) {
        webSocketSet.add(handler);
        sessionMap.put(session.getId(), handler);
        webSocketToBusinessSessionMap.put(session.getId(), businessSessionId);
    }

    public String unregister(Session session, RagWebSocketHandler handler) {
        String webSocketId = session.getId();
        String businessSessionId = webSocketToBusinessSessionMap.remove(webSocketId);
        webSocketSet.remove(handler);
        sessionMap.remove(webSocketId);
        return businessSessionId;
    }

    public String getBusinessSessionId(String webSocketSessionId) {
        return webSocketToBusinessSessionMap.get(webSocketSessionId);
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
        RagWebSocketHandler handler = sessionMap.get(sessionId);
        if (handler != null && handler.getSession() != null) {
            sendToSession(handler.getSession(), message);
        } else {
            log.warn("找不到会话ID为 {} 的WebSocket处理器", sessionId);
        }
    }

    public void broadcast(Object message) {
        for (RagWebSocketHandler item : webSocketSet) {
            try {
                sendToSession(item.getSession(), message);
            } catch (Exception e) {
                log.error("广播消息失败", e);
            }
        }
    }

    public int connectionCount() {
        return webSocketSet.size();
    }
}

