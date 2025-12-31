package yuuine.xxrag.vector.domain.embedding.service.impl;

import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.vector.domain.embedding.model.ResponseResult;
import yuuine.xxrag.vector.domain.embedding.service.EmbeddingService;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;
import yuuine.xxrag.vector.dto.request.VectorAddRequest;
import yuuine.xxrag.vector.dto.response.VectorAddResult;
import yuuine.xxrag.vector.dto.response.VectorChunk;
import yuuine.xxrag.vector.util.DashScopeEmbeddingUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * EmbeddingService 的实现：
 * <p>
 * 职责说明：
 * 1. 接收 rag-app 传入的 chunk 列表
 * 2. 批量调用 DashScope Embedding API
 * 3. 将 embedding 结果 + 原始 chunk 映射为 RagChunkDocument
 * 4. 构建 VectorAddResult，标记每个 chunk 的成功/失败状态
 * <p>
 * 1. 接收 rag-app 传入的 search 请求
 * 2. 调用 DashScope Vector Search API
 * 3. 将 embedding 结果 vector 返回
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final DashScopeEmbeddingUtil dashScopeEmbeddingUtil;

    /**
     * 每次向 DashScope 发送的最大 chunk 数量
     * text-embedding-v4 模型官方规定最大 chunk 数量为 10 个
     */
    private static final int BATCH_SIZE = 10;

    @Override
    public ResponseResult embedBatch(List<VectorAddRequest> chunks) {

        ResponseResult responseResult = new ResponseResult();
        VectorAddResult vectorAddResult = new VectorAddResult();

        // 用于返回给 ES 写入层
        List<RagChunkDocument> documents = new ArrayList<>();

        // 用于返回给 rag-app 的逐 chunk 状态
        List<VectorChunk> vectorChunks = new ArrayList<>();

        int successCount = 0;
        int failedCount = 0;

        // 空输入直接返回
        if (chunks == null || chunks.isEmpty()) {
            vectorAddResult.setSuccessChunk(0);
            vectorAddResult.setFailedChunk(0);
            vectorAddResult.setVectorChunks(vectorChunks);

            responseResult.setRagChunkDocuments(documents);
            responseResult.setVectorAddResult(vectorAddResult);
            return responseResult;
        }

        // 按 BATCH_SIZE 分批调用 embedding
        for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {

            int end = Math.min(i + BATCH_SIZE, chunks.size());
            List<VectorAddRequest> batch = chunks.subList(i, end);

            // 提取文本
            List<String> texts = batch.stream()
                    .map(VectorAddRequest::getChunkText)
                    .toList();

            try {
                // 1. 调用 DashScope Embedding API
                TextEmbeddingResult embeddingResult =
                        dashScopeEmbeddingUtil.generateEmbeddingResult(texts);

                List<TextEmbeddingResultItem> embeddings =
                        embeddingResult.getOutput().getEmbeddings();

                /*
                  2. 将 embedding 结果映射回原始 chunk
                  - 不能用 for (j) 直接 batch.get(j)
                  - 必须使用 item.getTextIndex()
                  因为 DashScope 不保证返回的 embedding 顺序 = 输入顺序
                 */
                for (TextEmbeddingResultItem item : embeddings) {

                    int textIndex = item.getTextIndex();
                    VectorAddRequest request = batch.get(textIndex);

                    // 原始向量类型 Double -> float[]
                    List<Double> doubleList = item.getEmbedding();
                    float[] vector = new float[doubleList.size()];
                    for (int k = 0; k < doubleList.size(); k++) {
                        vector[k] = doubleList.get(k).floatValue();
                    }

                    // 构建 ES 写模型 RagChunkDocument
                    RagChunkDocument document = new RagChunkDocument();
                    document.setChunkId(request.getChunkId());
                    document.setFileMd5(request.getFileMd5());
                    document.setSource(request.getSource());
                    document.setChunkIndex(request.getChunkIndex()); // 按照原文索引顺序
                    document.setContent(request.getChunkText());
                    document.setCharCount(request.getCharCount());
                    document.setEmbedding(vector);
                    document.setEmbeddingDim(vector.length);
                    document.setModel("text-embedding-v4");
                    document.setCreatedAt(Instant.now());

                    documents.add(document);
                }

                // 3. 本批次 embedding 成功 → 所有 chunk 标记成功
                for (VectorAddRequest req : batch) {
                    VectorChunk vc = new VectorChunk();
                    vc.setChunkId(req.getChunkId());
                    vc.setSuccess(true);
                    vc.setErrorMessage(null);

                    vectorChunks.add(vc);
                    successCount++;
                }

            } catch (Exception e) {

                log.error("Embedding batch failed", e);

                // 本批次 embedding 失败 → 所有 chunk 标记失败
                for (VectorAddRequest req : batch) {
                    VectorChunk vc = new VectorChunk();
                    vc.setChunkId(req.getChunkId());
                    vc.setSuccess(false);
                    vc.setErrorMessage("Embedding 调用失败: " + e.getMessage());

                    vectorChunks.add(vc);
                    failedCount++;
                }
            }
        }

        // 构建返回给 rag-app 的统计信息
        vectorAddResult.setSuccessChunk(successCount);
        vectorAddResult.setFailedChunk(failedCount);
        vectorAddResult.setVectorChunks(vectorChunks);

        // 构建最终 ResponseResult
        responseResult.setRagChunkDocuments(documents);
        responseResult.setVectorAddResult(vectorAddResult);

        log.info(
                "Embedding finished: successChunk={}, failedChunk={}, documents={}",
                successCount, failedCount, documents.size()
        );

        return responseResult;
    }

    @Override
    public float[] embedQuery(String query) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Query text cannot be null or empty");
        }

        try {
            // 调用 DashScope Embedding API，单条文本也用 list 包裹
            TextEmbeddingResult result = dashScopeEmbeddingUtil.generateEmbeddingResult(List.of(query));
            List<TextEmbeddingResultItem> embeddings = result.getOutput().getEmbeddings();

            if (embeddings.isEmpty()) {
                throw new RuntimeException("DashScope returned empty embedding");
            }

            // 取第一个 embedding，理论上只有一条返回
            List<Double> doubleList = embeddings.get(0).getEmbedding();
            float[] vector = new float[doubleList.size()];
            for (int i = 0; i < doubleList.size(); i++) {
                vector[i] = doubleList.get(i).floatValue();
            }

            return vector;

        } catch (Exception e) {
            log.error("Failed to generate query embedding", e);
            throw new RuntimeException("Query embedding failed: " + e.getMessage(), e);
        }
    }

}