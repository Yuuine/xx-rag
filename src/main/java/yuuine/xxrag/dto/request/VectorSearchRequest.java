package yuuine.xxrag.dto.request;

import lombok.Data;
import org.springframework.modulith.NamedInterface;

@Data
@NamedInterface("VectorSearchRequest")
public class VectorSearchRequest {

    /** 查询文本 */
    private String query;

    /** 返回 TopK，默认 5 */
    private Integer topK;
}