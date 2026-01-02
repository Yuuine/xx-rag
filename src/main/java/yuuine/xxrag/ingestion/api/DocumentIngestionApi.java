package yuuine.xxrag.ingestion.api;


import org.springframework.modulith.NamedInterface;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.response.IngestResponse;

import java.util.List;

@NamedInterface("ingestion-api")
public interface DocumentIngestionApi {

    IngestResponse ingest(List<MultipartFile> files);
}
