package yuuine.xxrag.vector.domain.es.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import yuuine.xxrag.rerank.service.RerankService;
import yuuine.xxrag.vector.config.RetrievalProperties;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;
import yuuine.xxrag.vector.domain.es.repository.RagChunkDocumentRepository;
import yuuine.xxrag.vector.util.RrfFusion;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagHybridSearchService {

    private final RagChunkDocumentRepository repository;
    private final RetrievalProperties properties;
    private final RerankService rerankService;

    public List<SearchHit<RagChunkDocument>> hybridSearch(
            String queryText,
            List<Float> queryVector
    ) {

        int recallTopK = properties.getRecallTopK();
        int numCandidates = recallTopK * properties.getCandidateMultiplier();
        int rrfTopK = properties.getRrf().getFinalTopK();

        log.info(
                "开始执行混合检索: queryText='{}', recallTopK={}, rrfTopK={}, numCandidates={}",
                queryText,
                recallTopK,
                rrfTopK,
                numCandidates
        );
        // 1. BM25
        List<SearchHit<RagChunkDocument>> textHits =
                repository.bm25Search(queryText)
                        .stream()
                        .limit(recallTopK)
                        .toList();

        log.info("BM25 搜索完成，返回结果数量: {}", textHits.size());

        // 2. kNN
        List<SearchHit<RagChunkDocument>> vectorHits =
                repository.knnSearch(queryVector, recallTopK, numCandidates);

        log.info("kNN 搜索完成，返回结果数量: {}", vectorHits.size());

        // 3. RRF 融合
        List<SearchHit<RagChunkDocument>> fused = RrfFusion.fuse(
                textHits,
                vectorHits,
                properties.getRrf().getK(),
                properties.getRrf().getTextWeight(),
                properties.getRrf().getVectorWeight()
        );

        log.info("RRF 融合完成，结果数量: {}", fused.size());

        // 4. RRF 候选集截断
        List<SearchHit<RagChunkDocument>> rrfResults =
                fused.stream()
                        .limit(rrfTopK)
                        .toList();

        // 5. Rerank 重排序（如果启用）
        if (properties.getRerank().isEnabled() && rerankService.isEnabled()) {
            return applyRerank(queryText, rrfResults);
        }

        log.info("混合检索完成，最终返回结果数量: {}", rrfResults.size());
        return rrfResults;
    }

    private List<SearchHit<RagChunkDocument>> applyRerank(
            String queryText,
            List<SearchHit<RagChunkDocument>> candidates
    ) {
        try {
            long startTime = System.currentTimeMillis();

            // 提取文档内容
            List<String> documents = candidates.stream()
                    .map(hit -> hit.getContent().getContent())
                    .collect(Collectors.toList());

            // 执行 Rerank
            List<Integer> rerankedIndices = rerankService.rerank(queryText, documents);
            int rerankTopK = properties.getRerank().getTopK();

            // 根据 Rerank 结果重新排序
            List<SearchHit<RagChunkDocument>> rerankedResults = rerankedIndices.stream()
                    .limit(rerankTopK)
                    .map(candidates::get)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.info("Rerank 完成，耗时: {}ms，输入: {}，输出: {}", 
                    duration, candidates.size(), rerankedResults.size());

            return rerankedResults;

        } catch (Exception e) {
            log.error("Rerank 失败，回退到原始顺序", e);
            return candidates.stream()
                    .limit(properties.getRerank().getTopK())
                    .collect(Collectors.toList());
        }
    }
}
