package yuuine.xxrag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class VectorAddRequest {

    private String chunkId;
    private String fileMd5;
    private String source;
    private Integer chunkIndex;

    private String chunkText;
    private Integer charCount;
}