package yuuine.xxrag.dto.request;

import lombok.Data;
import org.springframework.modulith.NamedInterface;

@Data
@NamedInterface("InferenceRequest")
public class InferenceRequest {

    private String query;

}