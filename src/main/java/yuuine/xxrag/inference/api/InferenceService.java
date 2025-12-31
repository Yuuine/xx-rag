package yuuine.xxrag.inference.api;


import org.springframework.modulith.NamedInterface;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.dto.response.InferenceResponse;

@NamedInterface("inference-api")
public interface InferenceService {

    InferenceResponse infer(InferenceRequest request);

}
