package yuuine.xxrag.app.docService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yuuine.xxrag.app.docService.DocService;
import yuuine.xxrag.app.docService.entity.RagDocuments;
import yuuine.xxrag.app.docService.repository.DocMapper;
import yuuine.xxrag.app.api.dto.response.DocList;
import yuuine.xxrag.app.exception.BusinessException;
import yuuine.xxrag.app.ragVectorService.RagVectorService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class DocServiceImpl implements DocService {

    private final DocMapper docMapper;
    private final RagVectorService ragVectorService;

    @Override
    public void saveDoc(String fileMd5, String fileName) {
        // 检查是否已存在相同MD5的文档
        int count = docMapper.countByFileMd5(fileMd5);
        if (count > 0) {
            throw new BusinessException("文件【" + fileName + "】已存在");
        }

        docMapper.saveDoc(fileMd5, fileName, LocalDateTime.now());
    }

    @Override
    public DocList getDoc() {

        List<RagDocuments> docs = docMapper.getDoc();

        DocList docList = new DocList();
        if (docs != null) {
            docList.setDocs(docs);
            log.info("获取文档列表成功，共计 {} 份", docs.size());
        } else {
            docList.setDocs(new ArrayList<>());
            log.info("文档列表为空");
        }

        return docList;
    }

    // DocServiceImpl.java
    @Override
    @Transactional // 可选：如果 DB 删除需事务（但向量删除不在事务内）
    public void deleteDocuments(List<String> fileMd5s) {
        log.info("开始批量删除文档，数量: {}", fileMd5s.size());


        int deletedCount = docMapper.batchDeleteByFileMd5(fileMd5s);
        log.info("MySQL 删除文档 {} 条", deletedCount);

        try {
            ragVectorService.deleteChunksByFileMd5s(fileMd5s);
            log.info("向量库 chunks 删除完成");
        } catch (Exception e) {
            log.warn("向量删除部分失败，将由后台任务补偿", e);
        }
    }

}
