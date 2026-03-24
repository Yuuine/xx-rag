package yuuine.xxrag.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 个人使用场景下的全局单人对话配置：进程内仅维护一条对话上下文，持久化到本地文件。
 * 非多租户/多会话隔离模型；若部署在公网或多人共用，请另行加强鉴权与隔离。
 */
@Configuration
@ConfigurationProperties(prefix = "app.chat.history")
@Data
public class ChatHistoryProperties {

    /**
     * 内存中消息条数达到该值时，整批写入历史文件并清空内存缓冲（与 {@link yuuine.xxrag.app.application.service.ChatSessionService} 一致）。
     */
    private int flushThreshold = 20;

    /**
     * 预留：个人版当前未使用（全局会话无按时间过期清理）。
     */
    private int sessionExpiryMinutes = 30;

    /**
     * 是否将历史写入本地文件；关闭后仅进程内保留，重启后不恢复。
     */
    private boolean persistenceEnabled = true;

    /**
     * 组装推理请求时，从全局历史中最多取最近多少条消息（防止上下文过长）。
     */
    private int maxHistoryMessages = 20;

    /**
     * 预留：前端历史回显条数（个人版 UI 可按需使用）。
     */
    private int maxHistoryEchoMessages = 20;

    /**
     * 历史记录持久化文件路径
     */
    private String historyFilePath = "./data/chat_history.json";
}
