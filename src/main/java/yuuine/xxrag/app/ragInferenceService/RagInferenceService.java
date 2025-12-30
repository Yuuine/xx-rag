package yuuine.xxrag.app.ragInferenceService;

import yuuine.ragapp.dto.request.InferenceRequest;
import yuuine.ragapp.dto.response.RagInferenceResponse;
import yuuine.ragapp.ragVectorService.VectorSearchResult;

import java.util.List;

public interface RagInferenceService {
    RagInferenceResponse inference(
            InferenceRequest query, List<VectorSearchResult> vectorSearchResults);
}
