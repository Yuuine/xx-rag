package yuuine.xxrag.app.application.service;

import yuuine.xxrag.app.application.dto.response.RagInferenceResponse;
import yuuine.xxrag.dto.common.VectorSearchResult;

import java.util.List;

public interface RagInferenceService {
    RagInferenceResponse inference(
            String query, List<VectorSearchResult> vectorSearchResults);
}
