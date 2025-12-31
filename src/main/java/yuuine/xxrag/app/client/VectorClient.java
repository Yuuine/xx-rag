package yuuine.xxrag.app.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import yuuine.xxrag.app.api.dto.request.InferenceRequest;
import yuuine.xxrag.VectorAddRequest;
import yuuine.xxrag.VectorAddResult;
import yuuine.xxrag.app.ragVectorService.VectorSearchResult;

import java.util.List;

@FeignClient(name = "rag-vector", url = "${services.vector.base-url}")
public interface VectorClient {

    @PostMapping(value = "/vectors/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    VectorAddResult add(
            @RequestBody List<VectorAddRequest> chunks);

    @PostMapping(value = "/vectors/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    List<VectorSearchResult> search(
            @RequestBody InferenceRequest query
    );

    @PostMapping(value = "/vectors/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    void deleteChunksByFileMd5s(
            @RequestBody List<String> fileMd5s
    );
}
