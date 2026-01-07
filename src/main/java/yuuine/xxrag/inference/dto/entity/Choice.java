package yuuine.xxrag.inference.dto.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import yuuine.xxrag.inference.dto.entity.Delta;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Choice {

    private Delta delta;

    @JsonProperty("finish_reason")
    private String finishReason;  // null 或 "stop" 等

    private Integer index;

    private Object logprobs;  // 示例中为 None
}