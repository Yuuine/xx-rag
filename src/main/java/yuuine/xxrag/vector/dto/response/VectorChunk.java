package yuuine.xxrag.vector.dto.response;

import lombok.Data;

@Data
public class VectorChunk {

    private String chunkId;
    private boolean success;
    private String errorMessage;
    // 注意：成功时不返回 vector，由后续索引服务处理
}
