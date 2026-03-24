package yuuine.xxrag.ingestion.api;

import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.response.IngestResponse;

import java.util.List;

public interface DocumentIngestionApi {

    IngestResponse ingest(List<MultipartFile> files);
}
