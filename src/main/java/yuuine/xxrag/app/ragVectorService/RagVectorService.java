package yuuine.xxrag.app.ragVectorService;

import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.dto.request.VectorAddRequest;
import yuuine.xxrag.dto.request.VectorSearchRequest;

import java.util.List;

public interface RagVectorService {

    VectorAddResult add(List<VectorAddRequest> chunks);

    List<VectorSearchResult> search(VectorSearchRequest query);

    void deleteChunksByFileMd5s(List<String> fileMd5s);
}
