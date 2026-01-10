package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yuuine.xxrag.app.application.service.DocService;
import yuuine.xxrag.app.application.service.impl.DocumentDeletionService;
import yuuine.xxrag.app.domain.model.RagDocuments;
import yuuine.xxrag.app.domain.repository.DocMapper;
import yuuine.xxrag.app.application.dto.response.DocList;
import yuuine.xxrag.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class DocServiceImpl implements DocService {

    private final DocMapper docMapper;
    private final DocumentDeletionService documentDeletionService;

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
        documentDeletionService.deleteDocuments(fileMd5s);
    }

}