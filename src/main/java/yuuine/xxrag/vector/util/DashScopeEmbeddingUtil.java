package yuuine.xxrag.vector.util;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashScopeEmbeddingUtil {

    private final String apiKey;
    private final String model;
    private final Integer dimension;

    public DashScopeEmbeddingUtil(
            @Value("${embedding.api.api-key}") String apiKey,
            @Value("${embedding.api.model:text-embedding-v4}") String model,
            @Value("${embedding.api.dimension:1024}") Integer dimension) {
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;

    }

    /**
     * 调用 DashScope 文本嵌入模型，生成嵌入向量
     *
     * @param texts 输入文本列表（建议控制在合理批量大小内）
     * @return TextEmbeddingResult 完整响应结果对象
     */
    public TextEmbeddingResult generateEmbeddingResult(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("输入文本列表不能为空");
        }

        try {
            TextEmbeddingParam param = TextEmbeddingParam
                    .builder()
                    .apiKey(apiKey)
                    .model(model)
                    .texts(texts)
                    .parameter("dimensions", dimension)
                    .build();

            TextEmbedding textEmbedding = new TextEmbedding();
            TextEmbeddingResult result = textEmbedding.call(param);

            return result;

        } catch (NoApiKeyException e) {
            throw new RuntimeException("DashScope API Key 未配置或无效，请检查 embedding.api.api-key 配置", e);
        } catch (ApiException e) {
            throw new RuntimeException("DashScope Embedding API 调用异常: " + e.getMessage(), e);
        }
    }
}