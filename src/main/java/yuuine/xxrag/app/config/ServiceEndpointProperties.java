package yuuine.xxrag.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "services")
@Data
public class ServiceEndpointProperties {

    private Endpoint ingestion;
    private Endpoint vector;
    private Endpoint inference;

    @Data
    public static class Endpoint {
        private String baseUrl;
    }
}

