package yuuine.xxrag.app.appService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.api.AppService;
import yuuine.xxrag.app.docService.DocService;
import yuuine.xxrag.Result;
import yuuine.xxrag.app.api.dto.request.InferenceRequest;
import yuuine.xxrag.app.api.dto.request.VectorAddRequest;
import yuuine.xxrag.app.api.dto.request.VectorAddResult;
import yuuine.xxrag.app.api.dto.response.DocList;
import yuuine.xxrag.app.api.dto.response.RagInferenceResponse;
import yuuine.xxrag.app.api.dto.response.RagIngestResponse;
import yuuine.xxrag.app.ragInferenceService.RagInferenceService;
import yuuine.xxrag.app.ragIngestService.RagIngestService;
import yuuine.xxrag.app.ragVectorService.RagVectorService;
import yuuine.xxrag.app.ragVectorService.VectorSearchResult;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
@Slf4j
public class AppServiceImpl implements AppService {

    private final RagIngestService ragIngestService;
    private final RagVectorService ragVectorService;
    private final RagInferenceService ragInferenceService;
    private final DocService docService;

    @Override
    public Result<Object> uploadFiles(List<MultipartFile> files) {
        log.info("收到上传请求，文件数量: {}", files.size());

        if (files == null || files.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 1. 调用 rag-ingestion 服务，得到 chunk 结果
        RagIngestResponse ragIngestResponse = ragIngestService.upload(files);
        log.debug("文件解析完成，chunks数量: {}", ragIngestResponse.getChunks().size());

        // 2. 调用 rag-vector 服务，持久化 chunk
        List<RagIngestResponse.ChunkResponse> chunkResponses = ragIngestResponse.getChunks();
        //类型转换
        List<VectorAddRequest> chunks = chunkResponses.stream()
                .map(chunk -> new VectorAddRequest(
                        chunk.getChunkId(),
                        chunk.getFileMd5(),
                        chunk.getSource(),
                        chunk.getChunkIndex(),
                        chunk.getChunkText(),
                        chunk.getCharCount()
                ))
                .toList();
        log.debug("准备向量存储，chunks数量: {}", chunks.size());
        VectorAddResult vectorAddResult = ragVectorService.add(chunks);

        // 3. 提取唯一文件并持久化到 MySQL
        Set<String> seenMd5s = new HashSet<>();
        for (var chunk : ragIngestResponse.getChunks()) {
            String md5 = chunk.getFileMd5();
            if (seenMd5s.contains(md5)) continue;

            docService.saveDoc(md5, chunk.getSource());
            seenMd5s.add(md5);
        }
        log.info("文件 MySQL 持久化完成，共 {} 个文件", seenMd5s.size());

        log.info("文件上传处理完成，成功: {}, 失败: {}",
                vectorAddResult.getSuccessChunk(), vectorAddResult.getFailedChunk());

        return Result.success(vectorAddResult);
    }

    @Override
    public Result<Object> getDocList() {
        DocList docList = docService.getDoc();
        return Result.success(docList);
    }

    @Override
    public Result<Object> deleteDocuments(List<String> fileMd5s) {
        if (fileMd5s == null || fileMd5s.isEmpty()) {
            return Result.error("fileMd5 列表不能为空");
        }
        docService.deleteDocuments(fileMd5s);
        return Result.success();
    }

    @Override
    public Result<Object> search(InferenceRequest query) {
        log.info("收到搜索请求，查询: {}", query.getQuery());

        if (query == null || query.getQuery() == null || query.getQuery().trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }

        // 1. 调用 rag-vector 服务，将搜索语句向量化处理，得到结果列表
        List<VectorSearchResult> vectorSearchResults = ragVectorService.search(query);

        // 2. 调用 rag-inference 服务，将问题和得到的结果列表传入 LLM 模型进行推理
        RagInferenceResponse ragInferenceResponse = ragInferenceService.inference(query, vectorSearchResults);

        // 3. 返回完整结果（包含答案和引用信息）
        return Result.success(ragInferenceResponse);
    }
}