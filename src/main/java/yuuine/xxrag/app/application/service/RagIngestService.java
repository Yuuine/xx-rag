package yuuine.xxrag.app.application.service;

import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.response.IngestResponse;

import java.util.List;

public interface RagIngestService {
    IngestResponse upload(List<MultipartFile> files);
}
