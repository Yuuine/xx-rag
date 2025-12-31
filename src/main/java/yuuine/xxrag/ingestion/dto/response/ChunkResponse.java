package yuuine.xxrag.ingestion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResponse {

    private String source;          // 原始文件名（如 "小王子.pdf"）
    private String fileMd5;         // 文件内容 MD5
    private String chunkId;         // UUID
    private Integer chunkIndex;     // 从 0 开始
    private String chunkText;       // 非空文本
    private Integer charCount;       // UTF-8 字符数
}
