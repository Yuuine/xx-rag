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

    private static AppApi appApi;
    private static ChatSessionService chatSessionService;
    private static RagWebSocketSessionManager wsSessionManager;

    @Getter
    private Session session;

    /**
     * 通过Spring上下文设置AppApi实例
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RagWebSocketHandler.appApi = applicationContext.getBean(AppApi.class);
        RagWebSocketHandler.chatSessionService = applicationContext.getBean(ChatSessionService.class);
        RagWebSocketHandler.wsSessionManager = applicationContext.getBean(RagWebSocketSessionManager.class);
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;

        // 生成或获取业务会话ID
        String businessSessionId = generateOrGetSessionId(session);
        wsSessionManager.register(session, this, businessSessionId);

        log.info("新的WebSocket连接，WebSocket ID: {}, 业务Session ID: {}，当前连接数: {}",
                session.getId(), businessSessionId, wsSessionManager.connectionCount());

        // 获取并发送最近的历史记录
        try {
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

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        String webSocketId = this.session.getId();
        String businessSessionId = wsSessionManager.unregister(this.session, this);

        log.info("WebSocket连接关闭，WebSocket ID: {}, 业务Session ID: {}，当前连接数: {}",
                webSocketId, businessSessionId, wsSessionManager.connectionCount());
    }

    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("来自客户端的消息: {}", message);

        String businessSessionId = wsSessionManager.getBusinessSessionId(session.getId());
        if (businessSessionId == null) {
            log.error("未找到业务会话ID，WebSocket ID: {}", session.getId());
            return;
        }

        try {
            // 将用户消息添加到会话历史
            chatSessionService.addUserMessage(businessSessionId, message);

            // 使用AppApi处理流式搜索
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

    /**
     * 发生错误时调用的方法
     */
    @OnError
    public void onError(Session session, Throwable error) {
        String businessSessionId = wsSessionManager.getBusinessSessionId(session.getId());
        log.error("WebSocket发生错误，WebSocket ID: {}, 业务Session ID: {}", session.getId(), businessSessionId, error);
    }

    /**
     * 向特定会话发送消息
     */
    public static void sendToSession(Session session, Object message) {
        wsSessionManager.sendToSession(session, message);
    }

    /**
     * 向指定会话ID发送消息
     */
    public static void sendMessageToSession(String sessionId, Object message) {
        wsSessionManager.sendMessageToSession(sessionId, message);
    }

    /**
     * 向所有连接的客户端广播消息
     */
    public static void broadcast(Object message) {
        wsSessionManager.broadcast(message);
    }

    /**
     * 生成或获取会话ID
     * 优先从 WebSocket 握手请求的查询参数中读取 sid（兼容带短横线和不带短横线的 UUID），
     * 如果提供且合法则使用该 sid，否则生成新的无短横线 UUID；
     * 这样同一浏览器（使用 localStorage 保存 sid）打开的多个标签页可共享同一 businessSessionId。
     */
    private String generateOrGetSessionId(Session session) {
        try {
            // 尝试从请求参数中获取 sid（query string 参数）
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

        // 如果没有可用的 sid，则生成一个新的无短横线 UUID
        String generated = UUID.randomUUID().toString().replace("-", "");
        log.debug("生成新会话ID: {}", generated);
        return generated;
    }

    /**
     * 验证并规范化 UUID 字符串：
     * - 支持带短横线的标准 UUID（8-4-4-4-12）
     * - 支持不带短横线的 32 个十六进制字符形式
     */
    private String normalizeAndValidateUuid(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return null;

        // 如果是 32 长度的十六进制字符串，插入短横线
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

        // 如果看起来已经是带短横线的 UUID，直接尝试解析
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

    /**
     * 获取业务会话ID
     */
    public static String getBusinessSessionId(String webSocketSessionId) {
        return wsSessionManager.getBusinessSessionId(webSocketSessionId);
    }
}
