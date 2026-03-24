package yuuine.xxrag.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import yuuine.xxrag.app.api.AppApi;
import yuuine.xxrag.app.application.service.ChatSessionService;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.dto.response.StreamResponse;

import java.util.*;

@Component
@ServerEndpoint("/ws-chat")
@Slf4j
public class RagWebSocketHandler implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Getter
    private Session session;

    private AppApi getAppApi() {
        return applicationContext.getBean(AppApi.class);
    }

    private ChatSessionService getChatSessionService() {
        return applicationContext.getBean(ChatSessionService.class);
    }

    private RagWebSocketSessionManager getWsSessionManager() {
        return applicationContext.getBean(RagWebSocketSessionManager.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        RagWebSocketHandler.applicationContext = ctx;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        RagWebSocketSessionManager wsSessionManager = getWsSessionManager();

        String businessSessionId = generateOrGetSessionId(session);
        wsSessionManager.register(session, this, businessSessionId);

        log.info("新的WebSocket连接，WebSocket ID: {}, 业务Session ID: {}，当前连接数: {}",
                session.getId(), businessSessionId, wsSessionManager.connectionCount());

        try {
            ChatSessionService chatSessionService = getChatSessionService();
            int historyLimit = chatSessionService != null ? chatSessionService.getMaxHistoryEchoMessages() : 20;
            List<InferenceRequest.Message> history = null;
            if (chatSessionService != null) {
                history = chatSessionService.getSessionHistory(businessSessionId, historyLimit);
            }
            if (history != null && !history.isEmpty()) {
                Map<String, Object> payload = getPayload(history);
                wsSessionManager.sendToSession(session, payload);
            }
        } catch (Exception e) {
            log.error("在 onOpen 时发送历史记录失败", e);
            wsSessionManager.sendToSession(session, createErrorPayload("历史记录加载失败"));
        }
    }

    @NotNull
    private static Map<String, Object> getPayload(List<InferenceRequest.Message> history) {
        List<Map<String, Object>> payloadMessages = new ArrayList<>();
        for (InferenceRequest.Message m : history) {
            Map<String, Object> mm = new HashMap<>();
            mm.put("role", m.getRole());
            mm.put("content", m.getContent());
            payloadMessages.add(mm);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "history");
        payload.put("messages", payloadMessages);
        return payload;
    }

    private Map<String, Object> createErrorPayload(String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "error");
        payload.put("message", message);
        return payload;
    }

    @OnClose
    public void onClose() {
        RagWebSocketSessionManager wsSessionManager = getWsSessionManager();
        String webSocketId = this.session.getId();
        String businessSessionId = wsSessionManager.unregister(this.session, this);

        log.info("WebSocket连接关闭，WebSocket ID: {}, 业务Session ID: {}，当前连接数: {}",
                webSocketId, businessSessionId, wsSessionManager.connectionCount());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("来自客户端的消息: {}", message);

        RagWebSocketSessionManager wsSessionManager = getWsSessionManager();
        String businessSessionId = wsSessionManager.getBusinessSessionId(session.getId());
        if (businessSessionId == null) {
            log.error("未找到业务会话ID，WebSocket ID: {}", session.getId());
            return;
        }

        try {
            ChatSessionService chatSessionService = getChatSessionService();
            chatSessionService.addUserMessage(businessSessionId, message);

            AppApi appApi = getAppApi();
            if (appApi == null) {
                wsSessionManager.sendToSession(session, StreamResponse.builder()
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
            wsSessionManager.sendToSession(session, StreamResponse.builder()
                    .content("")
                    .finishReason(null)
                    .message("处理请求时发生错误: " + e.getMessage())
                    .build());
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        RagWebSocketSessionManager wsSessionManager = getWsSessionManager();
        String businessSessionId = wsSessionManager.getBusinessSessionId(session.getId());
        log.error("WebSocket发生错误，WebSocket ID: {}, 业务Session ID: {}", session.getId(), businessSessionId, error);
    }

    public static void sendToSession(Session session, Object message) {
        getStaticWsSessionManager().sendToSession(session, message);
    }

    public static void sendMessageToSession(String sessionId, Object message) {
        getStaticWsSessionManager().sendMessageToSession(sessionId, message);
    }

    public static void broadcast(Object message) {
        getStaticWsSessionManager().broadcast(message);
    }

    public static String getBusinessSessionId(String webSocketSessionId) {
        return getStaticWsSessionManager().getBusinessSessionId(webSocketSessionId);
    }

    private static RagWebSocketSessionManager getStaticWsSessionManager() {
        return applicationContext.getBean(RagWebSocketSessionManager.class);
    }

    private String generateOrGetSessionId(Session session) {
        try {
            Map<String, List<String>> paramMap = session.getRequestParameterMap();
            if (paramMap != null) {
                List<String> sidList = paramMap.get("sid");
                if (sidList != null && !sidList.isEmpty()) {
                    String rawSid = sidList.get(0);
                    String normalized = normalizeAndValidateUuid(rawSid);
                    if (normalized != null) {
                        String dashless = normalized.replace("-", "");
                        log.debug("使用客户端提供的会话ID (normalized): {} -> stored as: {}", normalized, dashless);
                        return dashless;
                    } else {
                        log.warn("客户端提供的 sid 无效，将生成新的会话ID: {}", rawSid);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("尝试从请求参数读取 sid 时发生异常，将生成新的会话ID", e);
        }

        String generated = UUID.randomUUID().toString().replace("-", "");
        log.debug("生成新会话ID: {}", generated);
        return generated;
    }

    private String normalizeAndValidateUuid(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return null;

        if (trimmed.length() == 32 && trimmed.matches("[0-9a-fA-F]{32}")) {
            String withDashes = trimmed.replaceFirst("^([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})$",
                    "$1-$2-$3-$4-$5");
            try {
                UUID.fromString(withDashes);
                return withDashes.toLowerCase();
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        if (trimmed.length() == 36) {
            try {
                UUID.fromString(trimmed);
                return trimmed.toLowerCase();
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        return null;
    }
}