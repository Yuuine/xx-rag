package yuuine.xxrag.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import yuuine.xxrag.app.api.AppApi;
import yuuine.xxrag.app.application.service.ChatSessionService;
import yuuine.xxrag.dto.response.StreamResponse;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/ws-chat")
@Slf4j
public class RagWebSocketHandler implements ApplicationContextAware {

    private static AppApi appApi;
    private static ChatSessionService chatSessionService;

    // 存储所有连接的会话
    private static final CopyOnWriteArraySet<RagWebSocketHandler> webSocketSet = new CopyOnWriteArraySet<>();
    // 存储会话ID与处理器实例的映射关系
    private static final ConcurrentHashMap<String, RagWebSocketHandler> sessionMap = new ConcurrentHashMap<>();
    // 存储WebSocket会话与业务会话ID的映射关系
    private static final ConcurrentHashMap<String, String> webSocketToBusinessSessionMap = new ConcurrentHashMap<>();

    private Session session;

    /**
     * 通过Spring上下文设置AppApi实例
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RagWebSocketHandler.appApi = applicationContext.getBean(AppApi.class);
        RagWebSocketHandler.chatSessionService = applicationContext.getBean(ChatSessionService.class);
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        webSocketSet.add(this);
        sessionMap.put(session.getId(), this);

        // 生成或获取业务会话ID
        String businessSessionId = generateOrGetSessionId(session);
        webSocketToBusinessSessionMap.put(session.getId(), businessSessionId);

        log.info("新的WebSocket连接，WebSocket ID: {}, 业务Session ID: {}，当前连接数: {}",
                session.getId(), businessSessionId, webSocketSet.size());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        String webSocketId = this.session.getId();
        String businessSessionId = webSocketToBusinessSessionMap.remove(webSocketId);
        webSocketSet.remove(this);
        sessionMap.remove(webSocketId);

        log.info("WebSocket连接关闭，WebSocket ID: {}, 业务Session ID: {}，当前连接数: {}",
                webSocketId, businessSessionId, webSocketSet.size());
    }

    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("来自客户端的消息: {}", message);

        String businessSessionId = webSocketToBusinessSessionMap.get(session.getId());
        if (businessSessionId == null) {
            log.error("未找到业务会话ID，WebSocket ID: {}", session.getId());
            return;
        }

        try {
            // 将用户消息添加到会话历史
            chatSessionService.addUserMessage(businessSessionId, message);

            // 使用AppApi处理流式搜索
            if (appApi == null) {
                sendToSession(session, StreamResponse.builder()
                        .content("")
                        .finishReason(null)
                        .message("错误：AppApi未初始化")
                        .build());
                log.error("AppApi未初始化");
                return;
            }
            appApi.streamSearch(message, session.getId());
        } catch (Exception e) {
            log.error("处理消息时发生错误", e);
            sendToSession(session, StreamResponse.builder()
                    .content("")
                    .finishReason(null)
                    .message("处理请求时发生错误: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 发生错误时调用的方法
     */
    @OnError
    public void onError(Session session, Throwable error) {
        String businessSessionId = webSocketToBusinessSessionMap.get(session.getId());
        log.error("WebSocket发生错误，WebSocket ID: {}, 业务Session ID: {}", session.getId(), businessSessionId, error);
    }

    /**
     * 向特定会话发送消息
     */
    public static void sendToSession(Session session, Object message) {
        try {
            if (session != null && session.isOpen()) {
                String jsonMessage;
                if (message instanceof String) {
                    jsonMessage = (String) message;
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    jsonMessage = objectMapper.writeValueAsString(message);
                }
                session.getBasicRemote().sendText(jsonMessage);
            }
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 向指定会话ID发送消息
     */
    public static void sendMessageToSession(String sessionId, Object message) {
        RagWebSocketHandler handler = sessionMap.get(sessionId);
        if (handler != null && handler.session != null) {
            sendToSession(handler.session, message);
        } else {
            log.warn("找不到会话ID为 {} 的WebSocket处理器", sessionId);
        }
    }

    /**
     * 向所有连接的客户端广播消息
     */
    public static void broadcast(Object message) {
        for (RagWebSocketHandler item : webSocketSet) {
            try {
                sendToSession(item.session, message);
            } catch (Exception e) {
                log.error("广播消息失败", e);
            }
        }
    }

    /**
     * 生成或获取会话ID
     * 从WebSocket握手参数中获取，如果没有则生成新的
     */
    private String generateOrGetSessionId(Session session) {
        // 从握手参数中尝试获取会话ID
        String sessionId;
        // 如果没有获取到，则生成一个新的UUID
        sessionId = java.util.UUID.randomUUID().toString().replace("-", "");
        log.debug("生成新会话ID: {}", sessionId);

        return sessionId;
    }

    /**
     * 获取业务会话ID
     */
    public static String getBusinessSessionId(String webSocketSessionId) {
        return webSocketToBusinessSessionMap.get(webSocketSessionId);
    }
}
