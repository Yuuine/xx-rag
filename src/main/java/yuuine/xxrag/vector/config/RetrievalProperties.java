package yuuine.xxrag.vector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rag.retrieval")
@Data
public class RetrievalProperties {

    /**
     * 是否启用混合检索
     */
    private boolean hybridEnabled = true;

    /**
     * 文本 / 向量各自的召回数量
     */
    private int recallTopK = 20;

    /**
     * kNN 候选集倍数：numCandidates = recallTopK × candidateMultiplier
     */
    private int candidateMultiplier = 10;

    /**
     * RRF 融合参数
     */
    private Rrf rrf = new Rrf();

    /**
     * Rerank 参数
     */
    private Rerank rerank = new Rerank();

    @Data
    public static class Rrf {

        /**
         * RRF 平滑常数 k
         */
        private int k = 60;

        /**
         * BM25 权重
         */
        private double textWeight = 1.0;

        /**
         * 向量检索权重
         */
        private double vectorWeight = 1.0;

        /**
         * RRF 融合后返回数量（给 Rerank 用）
         */
        private int finalTopK = 20;
    }

    @Data
    public static class Rerank {

        /**
         * 是否启用 Rerank
         */
        private boolean enabled = false;

        /**
         * Rerank 后最终返回数量
         */
        private int topK = 5;
    }
}
