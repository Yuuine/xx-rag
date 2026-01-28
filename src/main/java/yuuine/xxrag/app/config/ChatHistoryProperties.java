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
}
