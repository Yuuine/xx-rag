package yuuine.xxrag.app.ragVectorService;

import yuuine.ragapp.dto.request.InferenceRequest;
import yuuine.ragapp.dto.request.VectorAddRequest;
import yuuine.ragapp.dto.request.VectorAddResult;

import java.util.List;

public interface RagVectorService {

    VectorAddResult add(List<VectorAddRequest> chunks);

    List<VectorSearchResult> search(InferenceRequest query);

    void deleteChunksByFileMd5s(List<String> fileMd5s);
}
