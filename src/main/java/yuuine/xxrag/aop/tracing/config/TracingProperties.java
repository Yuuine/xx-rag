package yuuine.xxrag.aop.tracing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aop.tracing")
public class TracingProperties {

    /**
     * 是否启用方法追踪功能
     */
    private boolean enabled = true;
}
