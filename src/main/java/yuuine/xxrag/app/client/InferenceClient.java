package yuuine.xxrag.app.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import yuuine.ragapp.dto.request.InferenceRequest;
import yuuine.ragapp.dto.response.InferenceResponse;

public interface InferenceClient {
    @PostExchange("/inference/chat")
    InferenceResponse chat(@RequestBody InferenceRequest request);
}
