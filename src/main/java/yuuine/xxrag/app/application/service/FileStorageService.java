package yuuine.xxrag.app.application.service;

import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.api.FileInfo;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public interface FileStorageService {

    String store(MultipartFile file, String fileMd5) throws IOException;

    Optional<File> getFile(String fileMd5);

    Optional<FileInfo> getFileInfo(String fileMd5);

    void delete(String fileMd5);

    boolean exists(String fileMd5);

    String getFileUrl(String fileMd5, String originalFilename);
}