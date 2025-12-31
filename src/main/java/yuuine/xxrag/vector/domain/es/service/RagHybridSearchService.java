package yuuine.xxrag.vector.domain.es.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import yuuine.xxrag.vector.config.RetrievalProperties;
import yuuine.xxrag.vector.domain.es.Repository.RagChunkDocumentRepository;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;
import yuuine.xxrag.vector.util.RrfFusion;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagHybridSearchService {

    private final RagChunkDocumentRepository repository;
    private final RetrievalProperties properties;

    public List<SearchHit<RagChunkDocument>> hybridSearch(
            String queryText,
            List<Float> queryVector
    ) {

        int recallTopK = properties.getRecallTopK();
        int numCandidates = recallTopK * properties.getCandidateMultiplier();
        int finalTopK = properties.getRrf().getFinalTopK();

        log.info(
                "开始执行混合检索: queryText='{}', recallTopK={}, finalTopK={}, numCandidates={}",
                queryText,
                recallTopK,
                finalTopK,
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

        log.info("混合检索完成，，RRF 融合后结果数量: {}", fused.size());
        // 4. rrf 候选集截断
        List<SearchHit<RagChunkDocument>> result =
                fused.stream()
                        .limit(finalTopK)
                        .toList();

        log.info("混合检索完成，最终返回结果数量: {}", result.size());
        return result;
    }
}
