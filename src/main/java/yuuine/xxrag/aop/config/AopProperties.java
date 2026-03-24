package yuuine.xxrag.aop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aop")
@Data
public class AopProperties {

    private FlowTrace flowTrace = new FlowTrace();

    private Performance performance = new Performance();

    @Data
    public static class FlowTrace {
        private boolean enabled = true;
        private boolean logRootOnly = true;
    }

    @Data
    public static class Performance {
        private boolean enabled = true;
        private long slowThresholdMs = 1000;
    }
}