package yuuine.xxrag.app.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import yuuine.xxrag.app.InferenceRequest;
import yuuine.xxrag.app.dto.response.InferenceResponse;

public interface InferenceClient {
    @PostExchange("/inference/chat")
    InferenceResponse chat(@RequestBody InferenceRequest request);
}
