package yuuine.xxrag.ingestion.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.response.IngestResponse;
import yuuine.xxrag.ingestion.application.assembler.ChunkAssembler;
import yuuine.xxrag.ingestion.domain.model.SingleFileProcessResult;
import yuuine.xxrag.ingestion.domain.service.ProcessSingleDocument;
import yuuine.xxrag.ingestion.api.DocumentIngestionApi;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentIngestionApiImpl implements DocumentIngestionApi {

    private final ProcessSingleDocument processSingleDocument;
    private final ChunkAssembler chunkAssembler;

    @Override
    public IngestResponse ingest(List<MultipartFile> files) {

        List<IngestResponse.ChunkResponse> allChunks = new ArrayList<>();
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
