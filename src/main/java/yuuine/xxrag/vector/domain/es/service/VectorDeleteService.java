// yuuine.ragvector.domain.es.service.VectorDeleteService.java
package yuuine.xxrag.vector.domain.es.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.vector.domain.es.Repository.RagChunkDocumentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDeleteService {

    private final RagChunkDocumentRepository ragChunkDocumentRepository;

    public void deleteChunksByFileMd5s(List<String> fileMd5s) {
        if (fileMd5s == null || fileMd5s.isEmpty()) {
            return;
        }

        log.debug("开始从 ES 删除 file: {}", fileMd5s);
        ragChunkDocumentRepository.deleteAllByFileMd5In(fileMd5s);
        log.info("ES 批量删除成功，file 数量: {}", fileMd5s.size());
    }
}