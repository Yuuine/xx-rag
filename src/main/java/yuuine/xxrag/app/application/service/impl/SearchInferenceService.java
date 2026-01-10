package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import yuuine.xxrag.app.application.service.RagVectorService;
import yuuine.xxrag.dto.common.ApiChatChunk;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.dto.response.StreamResponse;
import yuuine.xxrag.inference.api.InferenceService;
import yuuine.xxrag.websocket.RagWebSocketHandler;

import java.util.List;

/**
 * 搜索推理服务组件
 * 负责RAG流式搜索及WebSocket响应处理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchInferenceService {

    private final RagVectorService ragVectorService;
    private final InferenceService inferenceService;
    private final PromptConstructionService promptConstructionService;

    /**
     * 执行流式搜索
     */
    public void streamSearch(String query, String sessionId) {
        executeRagSearchStream(query, sessionId);
    }

    /**
     * 流式 RAG 搜索流程
     */
    private void executeRagSearchStream(String query, String sessionId) {
        try {
            log.info("开始流式搜索，查询: {}", query);

            String queryType = determineQueryType(query);
            boolean isChitChat = "闲聊".equals(queryType);

            List<VectorSearchResult> vectorResults = isChitChat
                    ? List.of()
                    : ragVectorService.search(query);

            log.info("流式查询类型: {}, 检索到 {} 条向量结果", queryType, vectorResults.size());

            // 构造推理请求
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
                            RagWebSocketHandler.sendMessageToSession(sessionId, increment);
                        }

                        // 发现结束标志则推送完成
                        String finishReason = choice.getFinishReason();
                        if (finishReason != null) {
                            StreamResponse end = StreamResponse.builder()
                                    .content("")
                                    .finishReason(finishReason)
                                    .message(null)
                                    .build();
                            RagWebSocketHandler.sendMessageToSession(sessionId, end);
                        }
                    },
                    error -> {
                        StreamResponse errorResponse = StreamResponse.builder()
                                .content("")
                                .finishReason(null)
                                .message("Error: " + error.getMessage())
                                .build();
                        RagWebSocketHandler.sendMessageToSession(sessionId, errorResponse);
                    },
                    () -> {
                        // Flux 正常完成时的兜底结束信号
                        StreamResponse complete = StreamResponse.builder()
                                .content("")
                                .finishReason("stop")
                                .message(null)
                                .build();
                        RagWebSocketHandler.sendMessageToSession(sessionId, complete);
                    }
            );

        } catch (Exception e) {
            log.error("流式搜索初始化失败", e);
            StreamResponse errorResponse = StreamResponse.builder()
                    .content("")
                    .finishReason(null)
                    .message("初始化失败: " + e.getMessage())
                    .build();
            RagWebSocketHandler.sendMessageToSession(sessionId, errorResponse);
        }
    }

    /**
     * 构造推理请求
     */
    private InferenceRequest buildInferenceRequest(String query, List<VectorSearchResult> contexts) {
        return promptConstructionService.buildInferenceRequest(query, contexts);
    }

    // 意图判断逻辑
    private String determineQueryType(String query) {
        String intentPrompt = promptConstructionService.buildIntentDetectionPrompt(query);

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