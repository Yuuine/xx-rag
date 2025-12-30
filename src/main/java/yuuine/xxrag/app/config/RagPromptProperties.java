package yuuine.xxrag.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag.prompt")
public class RagPromptProperties {
    private String systemPrompt = """
            你是一个专业的知识问答助手。请基于以下提供的检索到的相关文档内容，
            准确、完整地回答用户问题。
            如果文档中没有相关信息，请直接回答"我无法从提供的资料中找到答案"。
            请用自然、流畅的中文回复，不要提及"文档"或"检索"等技术术语。
            """;
}
