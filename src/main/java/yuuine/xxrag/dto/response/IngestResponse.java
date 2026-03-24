package yuuine.xxrag.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class IngestResponse {

    private List<ChunkResponse> chunks;
    private IngestSummary summary;

    @Data
    public static class ChunkResponse {
        private String source;
        private String fileMd5;
        private String chunkId;
        private Integer chunkIndex;
        private String chunkText;
        private Integer charCount;
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