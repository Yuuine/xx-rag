package yuuine.xxrag.app.ragVectorService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.client.VectorClient;
import yuuine.xxrag.dto.request.VectorAddRequest;
import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.request.VectorSearchRequest;
import yuuine.xxrag.app.exception.BusinessException;
import yuuine.xxrag.app.ragVectorService.RagVectorService;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.vector.api.VectorApi;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class RagVectorServiceImpl implements RagVectorService {

    private final VectorClient vectorClient;
    private final VectorApi vectorApi;

    @Override
    public VectorAddResult add(List<VectorAddRequest> chunks) {
        log.debug("开始添加向量数据，chunks数量: {}", chunks.size());
        log.info("向量数据添加，源文件数量: {}", chunks.stream().map(VectorAddRequest::getSource).distinct().count());

        try {
            VectorAddResult vectorAddResult = vectorApi.addVectors(chunks);
            if (vectorAddResult == null) {
                log.error("Vector服务返回空结果");
                throw new BusinessException("Vector服务返回空结果");
            }

            log.info("向量数据添加完成，成功: {}, 失败: {}", vectorAddResult.getSuccessChunk(), vectorAddResult.getFailedChunk());
            return vectorAddResult;
        } catch (Exception e) {
            log.error("向量数据添加失败", e);
            throw new BusinessException("Vector服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<VectorSearchResult> search(VectorSearchRequest query) {
        log.debug("开始向量搜索，查询: {}", query.getQuery());
        log.info("向量搜索请求，查询: {}", query.getQuery());

        try {
            VectorSearchRequest vectorSearchRequest = new VectorSearchRequest();
            vectorSearchRequest.setQuery(query.getQuery());
            vectorSearchRequest.setTopK(query.getTopK());
            
            List<VectorSearchResult> vectorSearchResults = vectorApi.search(vectorSearchRequest);
            if (vectorSearchResults == null) {
                log.error("Vector服务搜索返回空结果");
                throw new BusinessException("Vector服务搜索返回空结果");
            }

            log.debug("向量搜索完成，找到 {} 个结果", vectorSearchResults.size());
            return vectorSearchResults;
        } catch (Exception e) {
            log.error("向量搜索失败，查询: {}", query.getQuery(), e);
            throw new BusinessException("Vector服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteChunksByFileMd5s(List<String> fileMd5s) {

        vectorApi.deleteChunksByFileMd5s(fileMd5s);

    }
}
