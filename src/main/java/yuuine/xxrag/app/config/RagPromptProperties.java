package yuuine.xxrag.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag.prompt")
public class RagPromptProperties {
    private String knowledgeSystemPrompt;
    private String ordinarySystemPrompt;
}
