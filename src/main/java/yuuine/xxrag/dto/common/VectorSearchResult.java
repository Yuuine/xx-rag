package yuuine.xxrag.dto.common;

import lombok.Data;
import org.springframework.modulith.NamedInterface;

@Data
@NamedInterface("VectorSearchResult")
public class VectorSearchResult {

    private String chunkId;
    private String source;
    private Integer chunkIndex;
    private String content;
    private Float score;

}