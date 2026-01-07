package yuuine.xxrag.app.application.service;

import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.dto.request.VectorAddRequest;

import java.util.List;

public interface RagVectorService {

    VectorAddResult add(List<VectorAddRequest> chunks);

    List<VectorSearchResult> search(String query);

    void deleteChunksByFileMd5s(List<String> fileMd5s);
}
