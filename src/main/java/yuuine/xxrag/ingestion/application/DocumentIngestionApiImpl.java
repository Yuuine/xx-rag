package yuuine.xxrag.ingestion.application;

import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.response.IngestResponse;
import yuuine.xxrag.ingestion.application.assembler.ChunkAssembler;
import yuuine.xxrag.ingestion.domain.model.SingleFileProcessResult;
import yuuine.xxrag.ingestion.domain.service.ProcessSingleDocument;
import yuuine.xxrag.ingestion.api.DocumentIngestionApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DocumentIngestionApiImpl implements DocumentIngestionApi {

    private final ProcessSingleDocument processSingleDocument;
    private final ChunkAssembler chunkAssembler;

    private final TaskExecutor ioTaskExecutor;

    @Override
    public IngestResponse ingest(List<MultipartFile> files) {

        // 使用CompletableFuture并发处理所有文件
        List<CompletableFuture<SingleFileProcessResult>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> processSingleDocument.processSingleDocument(file),
                        ioTaskExecutor))
                .toList();

        // 等待所有处理完成
        List<SingleFileProcessResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        // 收集处理结果
        List<IngestResponse.ChunkResponse> allChunks = Collections.synchronizedList(new ArrayList<>());
        List<String> successFiles = Collections.synchronizedList(new ArrayList<>());
        List<String> failedFiles = Collections.synchronizedList(new ArrayList<>());

        // 分类处理结果
        for (SingleFileProcessResult result : results) {
            if (result.isSuccess()) {
                successFiles.add(result.getFilename());
                allChunks.addAll(chunkAssembler.toResponses(result));
            } else {
                failedFiles.add(result.getFilename());
            }
        }

        IngestResponse.FileResult fileResult = new IngestResponse.FileResult();
        fileResult.setSuccessfulFiles(successFiles);
        fileResult.setFailedFiles(failedFiles);

        IngestResponse.IngestSummary summary = new IngestResponse.IngestSummary();
        summary.setTotalFiles(files.size());
        summary.setFileResult(fileResult);

        IngestResponse response = new IngestResponse();
        response.setChunks(allChunks);
        response.setSummary(summary);

        return response;
    }
}
