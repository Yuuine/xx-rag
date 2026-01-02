package yuuine.xxrag.app.appService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.api.AppService;
import yuuine.xxrag.app.docService.DocService;
import yuuine.xxrag.app.dto.reponse.DocList;
import yuuine.xxrag.app.dto.reponse.RagInferenceResponse;

import yuuine.xxrag.app.ragInferenceService.RagInferenceService;
import yuuine.xxrag.app.ragIngestService.RagIngestService;
import yuuine.xxrag.app.ragVectorService.RagVectorService;
import yuuine.xxrag.dto.common.Result;
import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.dto.request.VectorAddRequest;
import yuuine.xxrag.dto.request.VectorSearchRequest;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.dto.response.IngestResponse;
import yuuine.xxrag.inference.api.InferenceService;
import yuuine.xxrag.dto.request.InferenceRequest;


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
    private final InferenceService inferenceService;

    @Override
    public Result<Object> uploadFiles(List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return Result.error("file not null");
        }

        // 1. 调用 rag-ingestion 服务，得到 chunk 结果
        IngestResponse ragIngestResponse = ragIngestService.upload(files);

        // 2. 调用 rag-vector 服务，持久化 chunk
        List<IngestResponse.ChunkResponse> chunkResponses = ragIngestResponse.getChunks();
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
    public Result<Object> search(VectorSearchRequest query) {
        log.info("收到搜索请求，查询: {}", query.getQuery());

        if (query.getQuery() == null || query.getQuery().trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }

        // 先判断查询类型，如果是闲聊则跳过向量检索
        log.debug("开始处理查询问题");
        String queryType = determineQueryType(query.getQuery());
        
        List<VectorSearchResult> vectorSearchResults;
        if ("闲聊".equals(queryType)) {
            // 闲聊类型不需要向量检索
            log.info("查询类型判断结果: {}，直接调用推理服务", queryType);
            vectorSearchResults = List.of();
        } else {
            // 知识查询需要向量检索
            log.info("查询类型判断结果: {}，开始向量检索", queryType);
            vectorSearchResults = ragVectorService.search(query);
        }

        RagInferenceResponse ragInferenceResponse = ragInferenceService.inference(query, vectorSearchResults);
        return Result.success(ragInferenceResponse);
    }
    
    private String determineQueryType(String query) {
        // 构建用于意图判断的提示词
        String intentPrompt = """
                请判断以下用户输入属于哪一类意图：
                知识查询：用户希望获取事实、操作步骤、定义、解释等具体信息。
                闲聊：用户进行问候、情感表达、无明确信息需求的对话。
                用户输入：%s
                你只需返回"闲聊"或者"知识查询"，不要返回除了这两个词语以外的任何内容。
                """.formatted(query);

        InferenceRequest intentRequest = new InferenceRequest();
        intentRequest.setQuery(intentPrompt);

        try {
            // 调用推理服务判断意图
            InferenceResponse intentResponse = inferenceService.infer(intentRequest);
            String result = intentResponse.getAnswer().trim();

            // 返回判断结果，只返回"闲聊"或"知识查询"
            if (result.contains("闲聊")) {
                return "闲聊";
            } else {
                return "知识查询";
            }
        } catch (Exception e) {
            log.warn("意图判断失败，将默认按知识查询处理: {}", e.getMessage());
            return "知识查询"; // 默认按知识查询处理
        }
    }
}