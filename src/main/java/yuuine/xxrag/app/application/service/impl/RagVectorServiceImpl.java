package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.application.service.RagVectorService;
import yuuine.xxrag.dto.request.VectorAddRequest;
import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.exception.BusinessException;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.vector.api.VectorApi;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class RagVectorServiceImpl implements RagVectorService {

    private final VectorApi vectorApi;

    @Override
    public VectorAddResult add(List<VectorAddRequest> chunks) {
        log.info("开始添加向量数据，chunks数量: {}，源文件数量: {}", chunks.size(), chunks.stream().map(VectorAddRequest::getSource).distinct().count());

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
    public List<VectorSearchResult> search(String query) {
        log.info("开始向量搜索，查询: {}", query);

        try {

            List<VectorSearchResult> vectorSearchResults = vectorApi.search(query);
            if (vectorSearchResults == null) {
                log.error("Vector服务搜索返回空结果");
                throw new BusinessException("Vector服务搜索返回空结果");
            }

            log.debug("向量搜索完成，找到 {} 个结果", vectorSearchResults.size());
            return vectorSearchResults;
        } catch (Exception e) {
            log.error("向量搜索失败，查询: {}", query, e);
            throw new BusinessException("Vector服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteChunksByFileMd5s(List<String> fileMd5s) {

        vectorApi.deleteChunksByFileMd5s(fileMd5s);

    }
}
