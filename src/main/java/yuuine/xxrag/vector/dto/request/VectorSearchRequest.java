package yuuine.xxrag.vector.dto.request;

import lombok.Data;

@Data
public class VectorSearchRequest {

    /** 查询文本 */
    private String query;

    /** 返回 TopK，默认 5 */
    private Integer topK;
}
