package yuuine.xxrag.app.docService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yuuine.xxrag.app.docService.DocService;
import yuuine.xxrag.app.docService.entity.RagDocuments;
import yuuine.xxrag.app.docService.repository.DocMapper;
import yuuine.xxrag.app.dto.reponse.DocList;
import yuuine.xxrag.exception.BusinessException;
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

    @Override
    @Transactional
    public void deleteDocuments(List<String> fileMd5s) {
        log.info("开始批量删除文档，数量: {}", fileMd5s.size());

        // 删除向量库
        try {
            ragVectorService.deleteChunksByFileMd5s(fileMd5s);
            log.info("向量库 chunks 删除完成");
        } catch (Exception e) {
            log.error("向量库删除失败，放弃数据库删除操作", e);
            throw new BusinessException("向量库删除失败，无法继续删除文档", e);
        }

        // 删除 MySQL（可回滚）
        int deletedCount = docMapper.batchDeleteByFileMd5(fileMd5s);
        log.info("MySQL 删除文档 {} 条", deletedCount);
    }

}
