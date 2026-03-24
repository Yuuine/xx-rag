package yuuine.xxrag.rerank.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankResult {
    
    /**
     * 原始索引
     */
    private int index;
    
    /**
     * 相关性分数 (0-1)
     */
    private float score;
    
    /**
     * 原始内容
     */
    private String content;
}
