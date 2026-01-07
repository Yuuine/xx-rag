package yuuine.xxrag.inference.dto.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionChunk {

    private String id;

    private String object = "chat.completion.chunk";

    private Long created;

    private String model;

    private List<Choice> choices;

    private Usage usage;  // 仅最后一个 chunk 非 null

    @JsonProperty("service_tier")
    private String serviceTier;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;
}