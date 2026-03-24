package yuuine.xxrag.vector.domain.embedding.model;

import lombok.Data;

@Data
public class VectorDocument {

    private String chunkId;             // ES_id
    private String fileMd5;
    private String source;
    private Integer chunkIndex;

    private String content;             // chunkText
    private Integer charCount;

    private float[] embedding;          // 向量（核心）
    private Integer embeddingDim;
    private String model;               // text-embedding-v4
    private Long createdAt;

}

