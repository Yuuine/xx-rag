package yuuine.xxrag.inference.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    private String apiKey;

    private String baseUrl;

    private String model;

    private Double temperature;

    private Integer maxTokens;

    private Integer timeoutSeconds;

}