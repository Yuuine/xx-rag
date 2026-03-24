package yuuine.xxrag.app.api;

import jakarta.websocket.Session;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.common.Result;

import java.util.List;

public interface AppApi {

    Result<Object> uploadFiles(List<MultipartFile> files);

    Result<Object> getDocList();

    Result<Object> deleteDocuments(List<String> fileMd5s);

    void streamSearch(String query, Session session);
}
