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
    private int recallTopK = 10;

    /**
     * kNN 候选集倍数：numCandidates = recallTopK × candidateMultiplier
     */
    private int candidateMultiplier = 10;

    /**
     * RRF 融合参数
     */
    private Rrf rrf = new Rrf();

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
         * RRF 融合后最终返回数量（截断）
         */
        private int finalTopK = 5;
    }
}
