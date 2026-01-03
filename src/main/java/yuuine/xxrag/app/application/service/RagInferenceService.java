package yuuine.xxrag.app.application.service;

import yuuine.xxrag.app.application.dto.response.RagInferenceResponse;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.dto.request.InferenceRequest;

import java.util.List;

public interface RagInferenceService {
    RagInferenceResponse inference(
            InferenceRequest query, List<VectorSearchResult> vectorSearchResults);
}
