package yuuine.xxrag.app.api.dto.request;

import lombok.Data;
import org.springframework.modulith.NamedInterface;


@Data
@NamedInterface("app-api-dto-request-InferenceRequest")
public class InferenceRequest {

    private String query;

    private Integer topK = 5;

}