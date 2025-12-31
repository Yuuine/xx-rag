package yuuine.xxrag.vector.domain.es.Repository;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;

import java.util.List;

@Repository
public interface RagChunkDocumentRepository
        extends ElasticsearchRepository<RagChunkDocument, String> {

    @Query("""
            {
              "knn": {
                "field": "embedding",
                "query_vector": ?0,
                "k": ?1,
                "num_candidates": ?2
              }
            }
            """)
    List<SearchHit<RagChunkDocument>> knnSearch(List<Float> vector, int k, int numCandidates);

    @Query("""
            {
              "match": {
                "content": {
                  "query": "?0"
                }
              }
            }
            """)
    List<SearchHit<RagChunkDocument>> bm25Search(String queryText);

    @Query("""
            {
              "terms": {
                "fileMd5": ?0
              }
            }
            """)
    void deleteAllByFileMd5In(List<String> fileMd5s);
}