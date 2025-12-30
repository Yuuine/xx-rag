package yuuine.xxrag.app.docService;

import yuuine.ragapp.dto.response.DocList;

import java.util.List;

public interface DocService {

    void saveDoc(String fileMd5, String fileName);

    DocList getDoc();

    void deleteDocuments(List<String> fileMd5s);
}
