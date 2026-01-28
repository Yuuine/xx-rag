package yuuine.xxrag.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;

@Data
@Component
@NamedInterface("admin-properties")
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    /**
     * 管理员清理所有会话记录的密码
     */
    private String cleanupPassword = "";
}
