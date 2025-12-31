package yuuine.xxrag.vector.domain.es.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.vector.domain.es.Repository.RagChunkDocumentRepository;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class VectorAddService {

    private final RagChunkDocumentRepository ragChunkDocumentRepository;

    public void saveAll(List<RagChunkDocument> documents) {

        log.info("开始保存向量文档到ES，文档数量: {}", documents.size());

        ragChunkDocumentRepository.saveAll(documents);

        log.info("成功保存向量文档到ES，文档数量: {}", documents.size());
    }

}
