package yuuine.xxrag.app.ragVectorService;

import yuuine.xxrag.*;

import java.util.List;

public interface RagVectorService {

    VectorAddResult add(List<VectorAddRequest> chunks);

    List<VectorSearchResult> search(VectorSearchRequest query);

    void deleteChunksByFileMd5s(List<String> fileMd5s);
}
