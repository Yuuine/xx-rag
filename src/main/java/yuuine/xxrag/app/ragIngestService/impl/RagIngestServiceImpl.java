package yuuine.xxrag.app.ragIngestService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.client.IngestionClient;
import yuuine.xxrag.app.api.dto.response.RagIngestResponse;
import yuuine.xxrag.app.exception.BusinessException;
import yuuine.xxrag.app.ragIngestService.RagIngestService;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class RagIngestServiceImpl implements RagIngestService {


    private final IngestionClient ingestionClient;

    @Override
    public RagIngestResponse upload(List<MultipartFile> files) {
        log.debug("开始上传文件到ingestion服务，文件数量: {}", files.size());
        log.info("上传文件到ingestion服务，文件名: {}", files.stream().map(MultipartFile::getOriginalFilename).toList());

        try {
            // 1. 将文件列表以 List<MultipartFile> files 的形式传入 app-ingestion
            // 2. 将得到的 chunks 封装返回 控制器，等待控制器下一步处理
            RagIngestResponse response = ingestionClient.ingest(files);
            if (response == null || response.getChunks() == null) {
                log.error("Ingestion服务返回空结果");
                throw new BusinessException("Ingestion服务返回空结果");
            }

            log.debug("Ingestion服务返回 {} 个chunks", response.getChunks().size());
            return response;
        } catch (Exception e) {
            log.error("上传文件到ingestion服务失败", e);
            throw new BusinessException("Ingestion服务调用失败: " + e.getMessage(), e);
        }
    }
}
