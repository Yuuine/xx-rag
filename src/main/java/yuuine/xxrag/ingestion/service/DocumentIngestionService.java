package yuuine.xxrag.ingestion.service;


import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.ingestion.dto.response.IngestResponse;

import java.util.List;

public interface DocumentIngestionService {

    IngestResponse ingest(List<MultipartFile> files);
}
