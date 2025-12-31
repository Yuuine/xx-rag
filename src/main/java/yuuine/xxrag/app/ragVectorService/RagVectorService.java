package yuuine.xxrag.app.ragVectorService;

import yuuine.xxrag.app.api.dto.request.InferenceRequest;
import yuuine.xxrag.VectorAddRequest;
import yuuine.xxrag.VectorAddResult;

import java.util.List;

public interface RagVectorService {

    VectorAddResult add(List<VectorAddRequest> chunks);

    List<VectorSearchResult> search(InferenceRequest query);

    void deleteChunksByFileMd5s(List<String> fileMd5s);
}
