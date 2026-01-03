// src/main/java/yuuine/xxrag/vector/application/VectorServiceImpl.java
package yuuine.xxrag.vector.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.vector.api.VectorApi;
import yuuine.xxrag.vector.domain.embedding.model.ResponseResult;
import yuuine.xxrag.vector.domain.embedding.service.EmbeddingService;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;
import yuuine.xxrag.vector.domain.es.service.VectorAddService;
import yuuine.xxrag.vector.domain.es.service.VectorDeleteService;
import yuuine.xxrag.vector.domain.es.service.VectorSearchService;
import yuuine.xxrag.dto.request.VectorAddRequest;
import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.common.VectorSearchResult;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorApiImpl implements VectorApi {

    private final EmbeddingService embeddingService;
    private final VectorAddService vectorAddService;
    private final VectorSearchService vectorSearchService;
    private final VectorDeleteService vectorDeleteService;

    @Override
    public VectorAddResult addVectors(List<VectorAddRequest> chunks) {
        log.info("接收到向量添加请求，待处理chunks数量: {}", chunks != null ? chunks.size() : 0);

        ResponseResult responseResult = embeddingService.embedBatch(chunks);
        List<RagChunkDocument> ragChunkDocuments = responseResult.getRagChunkDocuments();

        vectorAddService.saveAll(ragChunkDocuments);

        log.info("向量添加完成，成功: {}, 失败: {}",
                responseResult.getVectorAddResult().getSuccessChunk(),
                responseResult.getVectorAddResult().getFailedChunk());

        return responseResult.getVectorAddResult();
    }

    @Override
    public List<VectorSearchResult> search(InferenceRequest request) throws IOException {
        log.info("接收到向量搜索请求: query='{}'",
                request.getQuery() != null ? request.getQuery().substring(0, Math.min(request.getQuery().length(), 50)) + (request.getQuery().length() > 50 ? "..." : "") : null);

        List<VectorSearchResult> results = vectorSearchService.search(request);

        log.info("向量搜索完成，返回结果数量: {}", results.size());

        return results;
    }

    @Override
    public void deleteChunksByFileMd5s(List<String> fileMd5s) {
        if (fileMd5s == null || fileMd5s.isEmpty()) {
            log.warn("收到空的 fileMd5 列表，跳过删除");
            return;
        }
        log.info("接收到批量删除请求，fileMd5 数量: {}", fileMd5s.size());
        vectorDeleteService.deleteChunksByFileMd5s(fileMd5s);
        log.info("批量删除成功");
    }
}