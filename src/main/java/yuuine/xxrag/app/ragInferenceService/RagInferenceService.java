package yuuine.xxrag.app.ragInferenceService;

import yuuine.xxrag.dto.request.VectorSearchRequest;
import yuuine.xxrag.app.api.dto.response.RagInferenceResponse;
import yuuine.xxrag.dto.common.VectorSearchResult;

import java.util.List;

public interface RagInferenceService {
    RagInferenceResponse inference(
            VectorSearchRequest query, List<VectorSearchResult> vectorSearchResults);
}
