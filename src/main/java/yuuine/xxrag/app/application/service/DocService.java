package yuuine.xxrag.app.application.service;

import yuuine.xxrag.app.application.dto.response.DocList;
import yuuine.xxrag.app.domain.model.RagDocuments;

import java.util.List;
import java.util.Optional;

public interface DocService {

    void saveDoc(String fileMd5, String fileName);

    DocList getDoc();

    Optional<RagDocuments> getDocByMd5(String fileMd5);

    void deleteDocuments(List<String> fileMd5s);
}
