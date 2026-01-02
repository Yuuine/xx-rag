package yuuine.xxrag.ingestion.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.ingestion.domain.model.Chunk;
import yuuine.xxrag.ingestion.domain.model.DocumentProcessingContext;
import yuuine.xxrag.ingestion.domain.model.SingleFileProcessResult;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessSingleDocument {

    private final ProcessDocument processDocument;
    private final DocumentParserService documentParserService;
    private final ChunkService chunkService;

    public SingleFileProcessResult processSingleDocument(MultipartFile file) {

        String filename = file.getOriginalFilename();

        try {
            // 1. 构建上下文（MD5 / MIME）
            DocumentProcessingContext context = processDocument.processDocument(file);

            // 2. 解析为纯文本
            String plainText = documentParserService.parse(context);
            if (plainText == null || plainText.isBlank()) {
                log.warn("Parsed text is empty: {}", filename);
            }

            // 3. Chunk
            List<Chunk> chunks = chunkService.getChunks(plainText);
            if (chunks.isEmpty()) {
                log.warn("No chunks generated: {}", filename);
            }

            return SingleFileProcessResult.success(
                    filename,
                    context.getFileMd5(),
                    chunks
            );

        } catch (Exception e) {
            log.error("Process file failed: {}", filename, e);
            return SingleFileProcessResult.failure(
                    filename,
                    e.getMessage()
            );
        }
    }
}