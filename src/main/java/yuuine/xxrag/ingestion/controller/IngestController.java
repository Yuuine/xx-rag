package yuuine.xxrag.ingestion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.ingestion.dto.response.IngestResponse;
import yuuine.xxrag.ingestion.service.DocumentIngestionService;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/ingest")
public class IngestController {

    private final DocumentIngestionService documentIngestionService;

    @PostMapping()
    public IngestResponse ingest(
            @RequestParam("files") List<MultipartFile> files
    ) {

        return documentIngestionService.ingest(files);

    }
}
