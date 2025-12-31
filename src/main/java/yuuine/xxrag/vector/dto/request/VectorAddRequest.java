package yuuine.xxrag.vector.dto.request;

import lombok.Data;

@Data
public class VectorAddRequest {

    private String chunkId;
    private String fileMd5;
    private String source;
    private Integer chunkIndex;

    private String chunkText;
    private Integer charCount;
}

