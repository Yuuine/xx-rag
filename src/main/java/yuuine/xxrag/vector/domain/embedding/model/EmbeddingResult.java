package yuuine.xxrag.vector.domain.embedding.model;

import lombok.Data;
import java.util.List;

// DashScope Embedding API 向量化的返回结果
@Data
public class EmbeddingResult {
    private Output output;
    private String requestId;
    private Usage usage;

    @Data
    public static class Output {
        private List<EmbeddingItem> embeddings;
    }

    @Data
    public static class EmbeddingItem {
        private List<Float> embedding;
        private Integer textIndex;
    }

    @Data
    public static class Usage {
        private Integer totalTokens;
    }
}
