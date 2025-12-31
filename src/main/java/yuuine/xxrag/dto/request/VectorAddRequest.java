package yuuine.xxrag.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.modulith.NamedInterface;

@AllArgsConstructor
@NoArgsConstructor
@Data
@NamedInterface("vectorAddRequest")
public class VectorAddRequest {

    private String chunkId;
    private String fileMd5;
    private String source;
    private Integer chunkIndex;

    private String chunkText;
    private Integer charCount;
}