package yuuine.xxrag.inference.api;


import org.springframework.modulith.NamedInterface;
import reactor.core.publisher.Flux;
import yuuine.xxrag.dto.common.ApiChatChunk;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.dto.response.InferenceResponse;

@NamedInterface("inference-api")
public interface InferenceService {

    InferenceResponse infer(InferenceRequest request);

    Flux<ApiChatChunk> streamInfer(InferenceRequest request);
}
