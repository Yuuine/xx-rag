package yuuine.xxrag;

import lombok.Data;

@Data
public class VectorSearchResult {

    private String chunkId;
    private String source;
    private Integer chunkIndex;
    private String content;
    private Float score;

}