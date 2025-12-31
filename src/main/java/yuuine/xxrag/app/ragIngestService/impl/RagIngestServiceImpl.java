package yuuine.xxrag.app.ragIngestService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.client.IngestionClient;

import yuuine.xxrag.app.exception.BusinessException;
import yuuine.xxrag.app.ragIngestService.RagIngestService;
import yuuine.xxrag.ingestion.api.DocumentIngestionService;
import yuuine.xxrag.dto.response.IngestResponse;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class RagIngestServiceImpl implements RagIngestService {


    private final IngestionClient ingestionClient;
    private final DocumentIngestionService documentIngestionService;

    @Override
    public IngestResponse upload(List<MultipartFile> files) {

        try {
            // 1. 将文件列表 List<files> 传入 ingestion
            // 2. 将得到的 chunks 封装返回 控制器，等待控制器下一步处理
            IngestResponse response = documentIngestionService.ingest(files);

            if (response == null || response.getChunks() == null) {
                log.error("The ingestion service returned an empty result.");
                throw new BusinessException("The ingestion service returned an empty result.");
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to upload file to Ingestion service", e);
            throw new BusinessException("Ingestion service call failed:" + e.getMessage(), e);
        }
    }
}
