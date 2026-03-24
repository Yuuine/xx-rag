package yuuine.xxrag.vector.domain.es.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.stereotype.Service;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDeleteService {

    private final ElasticsearchOperations elasticsearchOperations;

    private static final String INDEX_NAME = "rag_chunks";

    public long deleteByFileMd5s(List<String> fileMd5s) {
        if (fileMd5s == null || fileMd5s.isEmpty()) {
            log.warn("fileMd5s 为空，跳过删除");
            return 0L;
        }

        // 1. 用 Spring Data 的 NativeQuery
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.terms(t -> t
                        .field("fileMd5")
                        .terms(v -> v.value(
                                fileMd5s.stream().map(FieldValue::of).toList()
                        ))
                ))
                .build();

        // 2. 构建 DeleteQuery
        DeleteQuery deleteQuery = DeleteQuery.builder(query)
                .withRefresh(true)
                .build();

        // 3. 执行 delete-by-query
        var response = elasticsearchOperations.delete(
                deleteQuery,
                RagChunkDocument.class,
                IndexCoordinates.of(INDEX_NAME)
        );

        long deleted = response.getDeleted();

        log.info("根据 {} 个 fileMd5 删除 {} 条 rag_chunks 文档", fileMd5s.size(), deleted);

        return deleted;
    }

    /*
      依赖 repository 的派生方法 deleteAllByFileMd5In
     */
//    @Deprecated
//    public void deleteChunksByFileMd5s(List<String> fileMd5s) {
//        if (fileMd5s == null || fileMd5s.isEmpty()) {
//            return;
//        }
//
//        log.warn("使用不推荐的 repository.deleteAllBy... 方式删除，可能不生效！");
//        ragChunkDocumentRepository.deleteAllByFileMd5In(fileMd5s);
//        log.info("尝试通过 repository 删除完成，file 数量: {}（请验证实际删除数量）", fileMd5s.size());
//    }
}