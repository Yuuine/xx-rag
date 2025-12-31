package yuuine.xxrag.app.ragVectorService;

import yuuine.xxrag.app.InferenceRequest;
import yuuine.xxrag.app.dto.request.VectorAddRequest;
import yuuine.xxrag.app.dto.request.VectorAddResult;

import java.util.List;

public interface RagVectorService {

    VectorAddResult add(List<VectorAddRequest> chunks);

    List<VectorSearchResult> search(InferenceRequest query);

    void deleteChunksByFileMd5s(List<String> fileMd5s);
}
