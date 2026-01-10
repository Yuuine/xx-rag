package yuuine.xxrag.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.modulith.NamedInterface;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@NamedInterface("StreamResponse")
public class StreamResponse {

    private String content; // 每次推送的实际文本片段

    private String finishReason; // 最后一个片段会置为 "stop"，其余为 null

    private String message; // 正常情况下为 null
}
