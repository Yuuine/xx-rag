package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import yuuine.xxrag.app.api.AppApi;
import yuuine.xxrag.app.application.service.DocService;
import yuuine.xxrag.app.application.dto.response.DocList;
import yuuine.xxrag.app.application.dto.response.RagInferenceResponse;

import yuuine.xxrag.app.application.service.RagInferenceService;
import yuuine.xxrag.app.application.service.RagIngestService;
import yuuine.xxrag.app.application.service.RagVectorService;
import yuuine.xxrag.dto.common.ApiChatChunk;
import yuuine.xxrag.dto.common.Result;
import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.dto.request.VectorAddRequest;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.dto.response.IngestResponse;
import yuuine.xxrag.dto.response.StreamResponse;
import yuuine.xxrag.inference.api.InferenceService;
import yuuine.xxrag.dto.request.InferenceRequest;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class AppApiImpl implements AppApi {

    private final RagIngestService ragIngestService;
    private final RagVectorService ragVectorService;
    private final RagInferenceService ragInferenceService;
    private final DocService docService;
    private final InferenceService inferenceService;
    private final SimpMessagingTemplate messagingTemplate;

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
    public Result<Object> search(String query) {
        try {
            RagInferenceResponse response = executeRagSearchBlocking(query);
            return Result.success(response);
        } catch (Exception e) {
            log.error("同步搜索处理失败", e);
            return Result.error("搜索处理失败: " + e.getMessage());
        }
    }

    @Override
    @Async("inferenceTaskExecutor")
    public CompletableFuture<Result<Object>> asyncSearch(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RagInferenceResponse response = executeRagSearchBlocking(query);
                return Result.success(response);
            } catch (Exception e) {
                log.error("异步搜索处理失败", e);
                return Result.error("异步搜索失败: " + e.getMessage());
            }
        });
    }

    @Override
    public void streamSearch(String query, String userDestination) {
        executeRagSearchStream(query, userDestination);
    }

    /**
     * 同步/阻塞式 RAG 搜索流程（供同步和异步接口使用）
     */
    private RagInferenceResponse executeRagSearchBlocking(String query) {
        log.info("收到搜索请求，查询: {}", query);

        String queryType = determineQueryType(query);
        boolean isChitChat = "闲聊".equals(queryType);

        List<VectorSearchResult> vectorResults = isChitChat
                ? List.of()
                : ragVectorService.search(query);

        log.info("查询类型: {}, 检索到 {} 条向量结果", queryType, vectorResults.size());

        return ragInferenceService.inference(query, vectorResults);
    }

    /**
     * 流式 RAG 搜索流程（WebSocket）
     */
    private void executeRagSearchStream(String query, String userDestination) {
        try {
            log.info("开始流式搜索，查询: {}", query);

            String queryType = determineQueryType(query);
            boolean isChitChat = "闲聊".equals(queryType);

            List<VectorSearchResult> vectorResults = isChitChat
                    ? List.of()
                    : ragVectorService.search(query);

            log.info("流式查询类型: {}, 检索到 {} 条向量结果", queryType, vectorResults.size());

            // 构造推理请求（可根据实际需要补充上下文）
            InferenceRequest request = buildInferenceRequest(query, vectorResults);

            Flux<ApiChatChunk> chunkFlux = inferenceService.streamInfer(request);

            chunkFlux.subscribe(
                    chunk -> {
                        if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
                            return;
                        }

                        ApiChatChunk.Choice choice = chunk.getChoices().get(0);
                        ApiChatChunk.Delta delta = choice.getDelta();

                        // 推送增量内容
                        String content = (delta != null && delta.getContent() != null)
                                ? delta.getContent()
                                : "";
                        if (!content.isEmpty()) {
                            StreamResponse increment = StreamResponse.builder()
                                    .content(content)
                                    .finishReason(null)
                                    .message(null)
                                    .build();
                            messagingTemplate.convertAndSend(userDestination, increment);
                        }

                        // 发现结束标志则推送完成
                        String finishReason = choice.getFinishReason();
                        if (finishReason != null) {
                            StreamResponse end = StreamResponse.builder()
                                    .content("")
                                    .finishReason(finishReason)
                                    .message(null)
                                    .build();
                            messagingTemplate.convertAndSend(userDestination, end);
                        }
                    },
                    error -> {
                        StreamResponse errorResponse = StreamResponse.builder()
                                .content("")
                                .finishReason(null)
                                .message("Error: " + error.getMessage())
                                .build();
                        messagingTemplate.convertAndSend(userDestination, errorResponse);
                    },
                    () -> {
                        // Flux 正常完成时的兜底结束信号
                        StreamResponse complete = StreamResponse.builder()
                                .content("")
                                .finishReason("stop")
                                .message(null)
                                .build();
                        messagingTemplate.convertAndSend(userDestination, complete);
                    }
            );

        } catch (Exception e) {
            log.error("流式搜索初始化失败", e);
            StreamResponse errorResponse = StreamResponse.builder()
                    .content("")
                    .finishReason(null)
                    .message("初始化失败: " + e.getMessage())
                    .build();
            messagingTemplate.convertAndSend(userDestination, errorResponse);
        }
    }

    /**
     * 构造推理请求
     */
    private InferenceRequest buildInferenceRequest(String query, List<VectorSearchResult> contexts) {
        InferenceRequest request = new InferenceRequest();
        request.setMessages(List.of(
                new InferenceRequest.Message("user", query)
        ));
        return request;
    }

    // 意图判断逻辑保持不变
    private String determineQueryType(String query) {
        String intentPrompt = """
                请判断以下用户输入属于哪一类意图：
                知识查询：用户希望获取事实、操作步骤、定义、解释等具体信息。
                闲聊：用户进行问候、情感表达、无明确信息需求的对话。
                你只需返回"闲聊"或者"知识查询"，不要返回除了这两个词语以外的任何内容。
                """;

        try {
            InferenceRequest intentRequest = new InferenceRequest();
            intentRequest.setMessages(List.of(
                    new InferenceRequest.Message("system", intentPrompt),
                    new InferenceRequest.Message("user", query)
            ));

            InferenceResponse intentResponse = inferenceService.infer(intentRequest);
            String result = intentResponse.getAnswer().trim();

            return result.contains("闲聊") ? "闲聊" : "知识查询";
        } catch (Exception e) {
            log.warn("意图判断失败，将默认按知识查询处理: {}", e.getMessage());
            return "知识查询";
        }
    }
}