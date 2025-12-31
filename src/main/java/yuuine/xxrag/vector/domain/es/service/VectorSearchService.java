package yuuine.xxrag.vector.domain.es.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import yuuine.xxrag.vector.config.RetrievalProperties;
import yuuine.xxrag.vector.domain.embedding.service.EmbeddingService;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;
import yuuine.xxrag.VectorSearchRequest;
import yuuine.xxrag.VectorSearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final EmbeddingService embeddingService;
    private final RagRetrievalService ragRetrievalService;
    private final RetrievalProperties retrievalProperties;

    public List<VectorSearchResult> search(VectorSearchRequest vectorSearchRequest) throws IOException {

        String query = vectorSearchRequest.getQuery();
        int topK = vectorSearchRequest.getTopK() != null ? vectorSearchRequest.getTopK() : 10;

        log.info("开始执行向量搜索: query={}, topK={}", query, topK);

        // 1. 生成查询向量
        float[] queryVectorArray = embeddingService.embedQuery(query);
        List<Float> queryVector = new ArrayList<>(queryVectorArray.length);
        for (float v : queryVectorArray) {
            queryVector.add(v);
        }
        log.debug("查询文本向量化完成，向量维度: {}", queryVectorArray.length);

        // 2. 统一检索
        List<SearchHit<RagChunkDocument>> searchHits =
                ragRetrievalService.search(query, queryVector);

        log.info("检索完成，返回结果数量: {}", searchHits.size());

        // 3. 结果转换
        List<VectorSearchResult> results = searchHits.stream().map(hit -> {
            RagChunkDocument doc = hit.getContent();
            VectorSearchResult r = new VectorSearchResult();
            r.setChunkId(doc.getChunkId());
            r.setSource(doc.getSource());
            r.setChunkIndex(doc.getChunkIndex());
            r.setContent(doc.getContent());
            r.setScore(hit.getScore());
            return r;
        }).collect(Collectors.toList());

        log.info("搜索结果转换完成，最终返回结果数量: {}", results.size());
        return results;
    }
}