package yuuine.xxrag.ingestion.domain.chunk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {

    private String chunkId;           // 全局唯一标识
    private Integer chunkIndex;       // 分块序号
    private String chunkText;         // chunk文本
    private Integer charCount;        // UTF-8 字符数

}
