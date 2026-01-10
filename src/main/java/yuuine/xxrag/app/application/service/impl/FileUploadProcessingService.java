package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.application.service.DocService;
import yuuine.xxrag.app.application.service.RagIngestService;
import yuuine.xxrag.app.application.service.RagVectorService;
import yuuine.xxrag.dto.common.Result;
import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.request.VectorAddRequest;
import yuuine.xxrag.dto.response.IngestResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件上传处理服务组件
 * 负责文件上传、解析、向量化存储等流程
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileUploadProcessingService {

    private final RagIngestService ragIngestService;
    private final RagVectorService ragVectorService;
    private final DocService docService;

    /**
     * 处理文件上传和存储的完整流程
     */
    public Result<Object> uploadFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Result.error("file not null");
        }

        // 1. 调用 rag-ingestion 服务，得到 chunk 结果
        IngestResponse ragIngestResponse = ragIngestService.upload(files);

        // 2. 调用 rag-vector 服务，持久化 chunk
        List<IngestResponse.ChunkResponse> chunkResponses = ragIngestResponse.getChunks();
        //类型转换
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
        VectorAddResult vectorAddResult = ragVectorService.add(chunks);

        // 3. 提取唯一文件并持久化到 MySQL
        Set<String> seenMd5s = new HashSet<>();
        for (var chunk : ragIngestResponse.getChunks()) {
            String md5 = chunk.getFileMd5();
            if (seenMd5s.contains(md5)) continue;

            docService.saveDoc(md5, chunk.getSource());
            seenMd5s.add(md5);
        }
        log.info("文件 MySQL 持久化完成，共 {} 个文件", seenMd5s.size());

        log.info("文件上传处理完成，成功: {}, 失败: {}",
                vectorAddResult.getSuccessChunk(), vectorAddResult.getFailedChunk());

        return Result.success(vectorAddResult);
    }
}