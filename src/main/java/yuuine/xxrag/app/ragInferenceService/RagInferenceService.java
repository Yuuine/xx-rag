package yuuine.xxrag.app.ragInferenceService;

import reactor.core.publisher.Flux;
import yuuine.xxrag.dto.common.StreamResult;
import yuuine.xxrag.dto.request.VectorSearchRequest;
import yuuine.xxrag.app.api.dto.response.RagInferenceResponse;
import yuuine.xxrag.dto.common.VectorSearchResult;

import java.util.List;

public interface RagInferenceService {
    RagInferenceResponse inference(
            VectorSearchRequest query, List<VectorSearchResult> vectorSearchResults);

    Flux<StreamResult<Object>> inferenceStream(VectorSearchRequest query, List<VectorSearchResult> results);
}
