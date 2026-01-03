package yuuine.xxrag.app.application.service;

import yuuine.xxrag.app.application.dto.response.DocList;

import java.util.List;

public interface DocService {

    void saveDoc(String fileMd5, String fileName);

    DocList getDoc();

    void deleteDocuments(List<String> fileMd5s);
}
