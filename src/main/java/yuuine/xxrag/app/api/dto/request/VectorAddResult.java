package yuuine.xxrag.app.api.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class VectorAddResult {

    private Integer successChunk = 0;
    private Integer failedChunk = 0;
    private List<VectorChunk> vectorChunks;

    @Data
    public static class VectorChunk {
        private String chunkId;
        private boolean success;
        private String errorMessage;
    }
}
