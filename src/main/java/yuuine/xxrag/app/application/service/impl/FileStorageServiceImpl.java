package yuuine.xxrag.app.application.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.api.FileInfo;
import yuuine.xxrag.app.application.service.DocService;
import yuuine.xxrag.app.application.service.FileStorageService;
import yuuine.xxrag.app.domain.model.RagDocuments;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    private final DocService docService;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("文件存储目录初始化完成: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("无法创建文件存储目录", e);
        }
    }

    @Override
    public String store(MultipartFile file, String fileMd5) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("无法存储空文件");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );

        String extension = getFileExtension(originalFilename);
        String storedFilename = fileMd5 + extension;

        Path destinationDir = rootLocation.resolve(fileMd5.substring(0, 2));
        Files.createDirectories(destinationDir);

        Path destinationFile = destinationDir.resolve(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("文件存储成功: {} -> {}", originalFilename, destinationFile);
        return destinationFile.toString();
    }

    @Override
    public Optional<File> getFile(String fileMd5) {
        try {
            String extension = findFileExtension(fileMd5);
            if (extension == null) {
                extension = "";
            }

            Path searchDir = rootLocation.resolve(fileMd5.substring(0, 2));
            Path filePath = searchDir.resolve(fileMd5 + extension);

            if (Files.exists(filePath)) {
                return Optional.of(filePath.toFile());
            }

            Files.list(searchDir)
                    .filter(path -> path.getFileName().toString().startsWith(fileMd5))
                    .findFirst()
                    .ifPresent(path -> {
                    });

            return Files.list(searchDir)
                    .filter(path -> path.getFileName().toString().startsWith(fileMd5))
                    .findFirst()
                    .map(Path::toFile);
        } catch (IOException e) {
            log.error("获取文件失败: {}", fileMd5, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String fileMd5) {
        try {
            String extension = findFileExtension(fileMd5);
            if (extension == null) {
                extension = "";
            }

            Path searchDir = rootLocation.resolve(fileMd5.substring(0, 2));
            Path filePath = searchDir.resolve(fileMd5 + extension);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", filePath);
            } else {
                log.warn("文件不存在，无法删除: {}", fileMd5);
            }
        } catch (IOException e) {
            log.error("删除文件失败: {}", fileMd5, e);
        }
    }

    @Override
    public boolean exists(String fileMd5) {
        return getFile(fileMd5).isPresent();
    }

    @Override
    public String getFileUrl(String fileMd5, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return "/xx/download/" + fileMd5 + extension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex);
    }

    private String findFileExtension(String fileMd5) {
        try {
            Path searchDir = rootLocation.resolve(fileMd5.substring(0, 2));
            if (!Files.exists(searchDir)) {
                return null;
            }

            return Files.list(searchDir)
                    .filter(path -> path.getFileName().toString().startsWith(fileMd5))
                    .findFirst()
                    .map(path -> {
                        String filename = path.getFileName().toString();
                        int dotIndex = filename.lastIndexOf('.');
                        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
                    })
                    .orElse(null);
        } catch (IOException e) {
            log.error("查找文件扩展名失败: {}", fileMd5, e);
            return null;
        }
    }

    public Path getRootLocation() {
        return rootLocation;
    }

    @Override
    public Optional<FileInfo> getFileInfo(String fileMd5) {
        try {
            Optional<File> fileOpt = getFile(fileMd5);
            if (fileOpt.isEmpty()) {
                log.warn("文件不存在，无法获取信息: {}", fileMd5);
                return Optional.empty();
            }

            File file = fileOpt.get();
            Path filePath = file.toPath();

            Optional<RagDocuments> docOpt = docService.getDocByMd5(fileMd5);
            if (docOpt.isEmpty()) {
                log.warn("文档元数据不存在: {}", fileMd5);
                return Optional.empty();
            }

            RagDocuments doc = docOpt.get();
            String filename = file.getName();
            String fileType = getFileType(filename);

            return Optional.of(FileInfo.builder()
                    .fileMd5(fileMd5)
                    .fileName(doc.getFileName())
                    .fileSize(Files.size(filePath))
                    .fileType(fileType)
                    .createdAt(doc.getCreatedAt())
                    .pageCount(null)
                    .charCount(null)
                    .chunkCount(null)
                    .build());
        } catch (Exception e) {
            log.error("获取文件信息失败: {}", fileMd5, e);
            return Optional.empty();
        }
    }

    private String getFileType(String filename) {
        String ext = getFileExtension(filename).toLowerCase();
        if (ext.isEmpty()) {
            return "application/octet-stream";
        }
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".doc", ".docx" -> "application/msword";
            case ".xls", ".xlsx" -> "application/vnd.ms-excel";
            case ".txt" -> "text/plain";
            case ".md" -> "text/markdown";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".html", ".htm" -> "text/html";
            case ".css" -> "text/css";
            case ".js" -> "application/javascript";
            case ".json" -> "application/json";
            case ".xml" -> "application/xml";
            default -> "application/octet-stream";
        };
    }
}