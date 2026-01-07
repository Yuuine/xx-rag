package yuuine.xxrag.inference.dto.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usage {

    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    @JsonProperty("completion_tokens")
    private Integer completionTokens;

    @JsonProperty("total_tokens")
    private Integer totalTokens;

    @JsonProperty("prompt_cache_hit_tokens")
    private Integer promptCacheHitTokens;

    @JsonProperty("prompt_cache_miss_tokens")
    private Integer promptCacheMissTokens;

    @JsonProperty("prompt_tokens_details")
    private Map<String, Integer> promptTokensDetails;
}