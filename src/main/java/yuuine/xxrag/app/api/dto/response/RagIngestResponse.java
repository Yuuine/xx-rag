package yuuine.xxrag.app.api.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class RagIngestResponse {

    private List<ChunkResponse> chunks;

    private IngestSummary summary;

    @Data
    public static class ChunkResponse {
        private String source;          // 原始文件名（如 "小王子.pdf"）
        private String fileMd5;         // 文件内容 MD5
        private String chunkId;         // UUID
        private Integer chunkIndex;     // 从 0 开始
        private String chunkText;       // 非空文本
        private Integer charCount;      // UTF-8 字符数
    }

    @Data
    public static class IngestSummary {
        private int totalFiles;
        private FileResult fileResult;
    }

    @Data
    public static class FileResult {
        private List<String> successfulFiles;
        private List<String> failedFiles;
    }
}
