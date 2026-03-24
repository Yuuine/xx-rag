package yuuine.xxrag.rerank.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.rag.retrieval.rerank")
public class RerankProperties {
    
    /**
     * 是否启用 Rerank
     */
    private boolean enabled = true;
    
    /**
     * Python Rerank 微服务地址
     */
    private String serviceUrl = "http://localhost:8082";
    
    /**
     * Rerank 后返回的最终数量
     */
    private int topK = 5;
}
