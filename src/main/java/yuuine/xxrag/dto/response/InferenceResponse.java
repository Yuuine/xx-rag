package yuuine.xxrag.dto.response;

import lombok.Data;
import org.springframework.modulith.NamedInterface;

@Data
@NamedInterface("InferenceResponse")
public class InferenceResponse {

    private String answer;

}