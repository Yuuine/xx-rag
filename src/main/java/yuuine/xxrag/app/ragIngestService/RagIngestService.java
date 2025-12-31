package yuuine.xxrag.app.ragIngestService;

import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.dto.response.RagIngestResponse;

import java.util.List;

public interface RagIngestService {
    RagIngestResponse upload(List<MultipartFile> files);
}
