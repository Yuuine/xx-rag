package yuuine.xxrag.dto.common;

import lombok.Data;
import org.springframework.modulith.NamedInterface;

import java.util.List;

@Data
@NamedInterface("VectorAddResult")
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