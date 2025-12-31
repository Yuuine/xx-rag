package yuuine.xxrag.vector.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class VectorAddResult {

    private Integer successChunk = 0;
    private Integer failedChunk = 0;
    private List<VectorChunk> vectorChunks;

}
