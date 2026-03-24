package yuuine.xxrag.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiChatChunk {

    private String id;

    private String object;

    private Long created;

    private String model;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    private List<Choice> choices;

    @Data
    public static class Choice {

        private Integer index;

        private Delta delta;

        private Object logprobs;

        @JsonProperty("finish_reason")
        private String finishReason; // 最后一个片段会置为 "stop"
    }

    @Data
    public static class Delta {

        private String content;
    }

}