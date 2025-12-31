package yuuine.xxrag.ingestion.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.ingestion.assembler.ChunkAssembler;
import yuuine.xxrag.ingestion.domain.models.SingleFileProcessResult;
import yuuine.xxrag.ingestion.domain.service.ProcessSingleDocument;
import yuuine.xxrag.ingestion.dto.response.ChunkResponse;
import yuuine.xxrag.ingestion.dto.response.FileResult;
import yuuine.xxrag.ingestion.dto.response.IngestResponse;
import yuuine.xxrag.ingestion.dto.response.IngestSummary;
import yuuine.xxrag.ingestion.service.DocumentIngestionService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private final ProcessSingleDocument processSingleDocument;
    private final ChunkAssembler chunkAssembler;

    @Override
    public IngestResponse ingest(List<MultipartFile> files) {

        List<ChunkResponse> allChunks = new ArrayList<>();
        List<String> successFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (MultipartFile file : files) {

            SingleFileProcessResult result = processSingleDocument.processSingleDocument(file);

            if (result.isSuccess()) {
                successFiles.add(result.getFilename());
                allChunks.addAll(chunkAssembler.toResponses(result));
            } else {
                failedFiles.add(result.getFilename());
            }
        }

        FileResult fileResult = new FileResult();
        fileResult.setSuccessfulFiles(successFiles);
        fileResult.setFailedFiles(failedFiles);

        IngestSummary summary = new IngestSummary();
        summary.setTotalFiles(files.size());
        summary.setFileResult(fileResult);

        IngestResponse response = new IngestResponse();
        response.setChunks(allChunks);
        response.setSummary(summary);

        return response;
    }
}
