package yuuine.xxrag.aop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aop")
@Data
public class AopProperties {

    /**
     * 是否启用流程追踪功能
     */
    private boolean flowTraceEnabled = true;

    /**
     * 是否启用性能监控功能
     */
    private boolean performanceEnabled = true;
}