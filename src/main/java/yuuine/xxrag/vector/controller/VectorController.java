package yuuine.xxrag.vector.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import yuuine.xxrag.vector.domain.embedding.model.ResponseResult;
import yuuine.xxrag.vector.domain.embedding.service.EmbeddingService;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;
import yuuine.xxrag.vector.domain.es.service.VectorAddService;
import yuuine.xxrag.vector.domain.es.service.VectorDeleteService;
import yuuine.xxrag.vector.domain.es.service.VectorSearchService;
import yuuine.xxrag.vector.dto.request.VectorAddRequest;
import yuuine.xxrag.vector.dto.request.VectorSearchRequest;
import yuuine.xxrag.vector.dto.response.VectorAddResult;
import yuuine.xxrag.vector.dto.response.VectorSearchResult;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/vectors")
public class VectorController {

    private final EmbeddingService embeddingService;
    private final VectorAddService vectorAddService;
    private final VectorSearchService vectorSearchService;
    private final VectorDeleteService vectorDeleteService;

    @PostMapping("/add")
    public VectorAddResult add(
            @RequestBody List<VectorAddRequest> chunks) {

        log.info("接收到向量添加请求，待处理chunks数量: {}", chunks != null ? chunks.size() : 0);

        // 将 chunks 进行向量化处理，得到一个 List<RagChunkDocument> 对象和处理结果对象，包含所有的 chunk 的向量化结果
        ResponseResult responseResult = embeddingService.embedBatch(chunks);
        List<RagChunkDocument> ragChunkDocuments = responseResult.getRagChunkDocuments();

        // 将 List<RagChunkDocument> 对象保存到 ES 中
        vectorAddService.saveAll(ragChunkDocuments);

        log.info("向量添加完成，成功: {}, 失败: {}",
                responseResult.getVectorAddResult().getSuccessChunk(),
                responseResult.getVectorAddResult().getFailedChunk());

        return responseResult.getVectorAddResult();
    }

    @PostMapping("/search")
    public List<VectorSearchResult> search(
            @RequestBody VectorSearchRequest vectorSearchRequest) throws IOException {

        log.info("接收到向量搜索请求: query='{}', topK={}",
                vectorSearchRequest.getQuery() != null ? vectorSearchRequest.getQuery().substring(0, Math.min(vectorSearchRequest.getQuery().length(), 50)) + (vectorSearchRequest.getQuery().length() > 50 ? "..." : "") : null,
                vectorSearchRequest.getTopK());

        List<VectorSearchResult> results = vectorSearchService.search(vectorSearchRequest);

        log.info("向量搜索完成，返回结果数量: {}", results.size());

        return results;
    }

    @PostMapping("/delete")
    public void delete(@RequestBody List<String> fileMd5s) {
        if (fileMd5s == null || fileMd5s.isEmpty()) {
            log.warn("收到空的 fileMd5 列表，跳过删除");
            return;
        }
        log.info("接收到批量删除请求，fileMd5 数量: {}", fileMd5s.size());
        vectorDeleteService.deleteByFileMd5s(fileMd5s);
        log.info("批量删除成功");
    }

}
