package yuuine.xxrag.vector.domain.es.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import yuuine.xxrag.vector.config.RetrievalProperties;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;
import yuuine.xxrag.vector.domain.es.repository.RagChunkDocumentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final RagChunkDocumentRepository repository;
    private final RetrievalProperties properties;
    private final RagHybridSearchService ragHybridSearchService;

    /**
     * 统一检索入口，根据配置自动选择纯 kNN 或混合检索
     */
    public List<SearchHit<RagChunkDocument>> search(
            String queryText,
            List<Float> queryVector
    ) {

        if (properties.isHybridEnabled()) {
            return ragHybridSearchService.hybridSearch(
                    queryText,
                    queryVector
            );
        } else {
            return repository.knnSearch(
                    queryVector,
                    properties.getRecallTopK(),
                    properties.getCandidateMultiplier()
            );
        }
    }
}