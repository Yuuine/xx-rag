package yuuine.xxrag.app.ragInferenceService;

import yuuine.xxrag.app.api.dto.request.InferenceRequest;
import yuuine.xxrag.app.api.dto.response.RagInferenceResponse;
import yuuine.xxrag.app.ragVectorService.VectorSearchResult;

import java.util.List;

public interface RagInferenceService {
    RagInferenceResponse inference(
            InferenceRequest query, List<VectorSearchResult> vectorSearchResults);
}
