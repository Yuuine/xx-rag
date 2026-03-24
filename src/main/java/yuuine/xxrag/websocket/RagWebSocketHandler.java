package yuuine.xxrag.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import yuuine.xxrag.app.application.service.ChatSessionService;
import yuuine.xxrag.app.application.service.impl.SearchInferenceService;
import yuuine.xxrag.dto.response.StreamResponse;

/**
 * 个人使用场景 WebSocket：所有连接共享全局对话与检索流程；同一用户多标签页会收到独立连接但共用一份会话历史。
 */
@Component
@ServerEndpoint("/ws-chat")
@Slf4j
public class RagWebSocketHandler implements ApplicationContextAware {

    private static ChatSessionService chatSessionService;
    private static SearchInferenceService searchInferenceService;
    private static RagWebSocketSessionManager wsSessionManager;

    @Getter
    private Session session;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RagWebSocketHandler.chatSessionService = applicationContext.getBean(ChatSessionService.class);
        RagWebSocketHandler.searchInferenceService = applicationContext.getBean(SearchInferenceService.class);
        RagWebSocketHandler.wsSessionManager = applicationContext.getBean(RagWebSocketSessionManager.class);
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        wsSessionManager.register(session);

        log.info("WebSocket 连接已建立，ID: {}，当前连接数: {}",
                session.getId(), wsSessionManager.connectionCount());
    }

    @OnClose
    public void onClose() {
        wsSessionManager.unregister(session);
        log.info("WebSocket 连接关闭，ID: {}，当前连接数: {}",
                session.getId(), wsSessionManager.connectionCount());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.debug("收到客户端消息，长度: {}", message != null ? message.length() : 0);

        try {
            chatSessionService.addUserMessage(message);
            searchInferenceService.streamSearch(message, session);
        } catch (Exception e) {
            log.error("处理消息时发生错误", e);
            sendToSession(session, StreamResponse.builder()
                    .content("")
                    .finishReason(null)
                    .message("处理请求时发生错误: " + e.getMessage())
                    .build());
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 发生错误，ID: {}", session.getId(), error);
    }

    public static void sendToSession(Session session, Object message) {
        wsSessionManager.sendToSession(session, message);
    }

    public static void broadcast(Object message) {
        wsSessionManager.broadcast(message);
    }
}
