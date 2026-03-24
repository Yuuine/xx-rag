package yuuine.xxrag.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.chat.history")
@Data
public class ChatHistoryProperties {

    /**
     * 缓存刷新阈值 - 当缓存中的消息数量达到此值时，自动刷新到数据库
     */
    private int flushThreshold = 10;

    /**
     * 会话过期时间（分钟）- 会话在指定时间内无活动将被清理
     */
    private int sessionExpiryMinutes = 30;

    /**
     * 是否启用数据库持久化
     */
    private boolean persistenceEnabled = true;
    /**
     * 最大历史消息数量 - 单次请求返回的最大历史消息数（用于发送给模型，防止 token 爆炸）
     */
    private int maxHistoryMessages = 10;

    /**
     * 历史回显数量 - 首次连接时向前端回显的历史消息条数（UI 展示）
     */
    private int maxHistoryEchoMessages = 20;
}
