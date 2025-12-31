package yuuine.xxrag.app.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.response.IngestResponse;

import java.util.List;

@FeignClient(name = "rag-ingestion", url = "${services.ingestion.base-url}")
public interface IngestionClient {

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    IngestResponse ingest(
            @RequestPart("files") List<MultipartFile> files
    );
}
