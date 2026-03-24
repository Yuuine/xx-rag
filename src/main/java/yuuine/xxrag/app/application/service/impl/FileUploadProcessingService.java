package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.application.service.DocService;
import yuuine.xxrag.app.application.service.FileStorageService;
import yuuine.xxrag.app.application.service.RagIngestService;
import yuuine.xxrag.app.application.service.RagVectorService;
import yuuine.xxrag.common.constant.FileConstants;
import yuuine.xxrag.dto.common.Result;
import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.request.VectorAddRequest;
import yuuine.xxrag.dto.response.IngestResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileUploadProcessingService {

    private final RagIngestService ragIngestService;
    private final RagVectorService ragVectorService;
    private final DocService docService;
    private final FileStorageService fileStorageService;

    public Result<Object> uploadFiles(List<MultipartFile> files) {
        Result<Object> validationResult = validateFiles(files);
        if (validationResult != null) {
            return validationResult;
        }

        IngestResponse ragIngestResponse = ragIngestService.upload(files);
        VectorAddResult vectorAddResult = addVectors(ragIngestResponse);
        saveDocuments(ragIngestResponse);
        storeOriginalFiles(files, ragIngestResponse);

        log.info("文件上传处理完成，成功: {}, 失败: {}",
                vectorAddResult.getSuccessChunk(), vectorAddResult.getFailedChunk());

        return Result.success(vectorAddResult);
    }

    private Result<Object> validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Result.error("file not null");
        }

        for (MultipartFile file : files) {
            if (file.getSize() > FileConstants.MAX_FILE_SIZE) {
                return Result.error("文件大小超过限制（最大100MB）");
            }

            String contentType = file.getContentType();
            if (contentType == null || !FileConstants.ALLOWED_FILE_TYPES.contains(contentType)) {
                return Result.error("不支持的文件类型");
            }

            if (file.isEmpty()) {
                return Result.error("文件内容为空");
            }
        }
        return null;
    }

    private VectorAddResult addVectors(IngestResponse ragIngestResponse) {
        List<IngestResponse.ChunkResponse> chunkResponses = ragIngestResponse.getChunks();
        List<VectorAddRequest> chunks = chunkResponses.stream()
                .map(chunk -> new VectorAddRequest(
                        chunk.getChunkId(),
                        chunk.getFileMd5(),
                        chunk.getSource(),
                        chunk.getChunkIndex(),
                        chunk.getChunkText(),
                        chunk.getCharCount()
                ))
                .collect(Collectors.toList());
        return ragVectorService.add(chunks);
    }

    private void saveDocuments(IngestResponse ragIngestResponse) {
        Set<String> seenMd5s = new HashSet<>();
        for (var chunk : ragIngestResponse.getChunks()) {
            String md5 = chunk.getFileMd5();
            if (seenMd5s.contains(md5)) continue;

            docService.saveDoc(md5, chunk.getSource());
            seenMd5s.add(md5);
        }
        log.info("文件 MySQL 持久化完成，共 {} 个文件", seenMd5s.size());
    }

    private void storeOriginalFiles(List<MultipartFile> files, IngestResponse ragIngestResponse) {
        Set<String> storedMd5s = ragIngestResponse.getChunks().stream()
                .map(IngestResponse.ChunkResponse::getFileMd5)
                .collect(Collectors.toSet());

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            if (filename == null) continue;

            String fileMd5 = computeMd5(file);
            if (fileMd5 == null || !storedMd5s.contains(fileMd5)) continue;

            try {
                fileStorageService.store(file, fileMd5);
                log.info("原文件已存储: {}", filename);
            } catch (Exception e) {
                log.error("存储原文件失败: {}", filename, e);
            }
        }
    }

    private String computeMd5(MultipartFile file) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算 MD5 失败", e);
            return null;
        }
    }
}