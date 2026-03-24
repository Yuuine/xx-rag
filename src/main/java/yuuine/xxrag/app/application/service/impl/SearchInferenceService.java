package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import yuuine.xxrag.app.application.service.ChatSessionService;
import yuuine.xxrag.app.application.service.RagVectorService;
import yuuine.xxrag.dto.common.ApiChatChunk;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.dto.response.StreamResponse;
import yuuine.xxrag.inference.api.InferenceService;
import yuuine.xxrag.websocket.RagWebSocketHandler;

import java.util.ArrayList;
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
    private final ChatSessionService chatSessionService;

    /**
     * 执行流式搜索
     */
    public void streamSearch(String query, String userDestination) {
        executeRagSearchStream(query, userDestination);
    }

    /**
     * 流式 RAG 搜索流程
     */
    private void executeRagSearchStream(String query, String userDestination) {
        try {
            log.info("开始流式搜索，查询: {}", query);

            String businessSessionId = RagWebSocketHandler.getBusinessSessionId(userDestination);
            if (businessSessionId == null) {
                log.error("无法获取业务会话ID，WebSocket ID: {}", userDestination);
                return;
            }

            boolean isChitChat = isChitChatQuery(query);
            List<VectorSearchResult> contexts = isChitChat ? null : searchContexts(query);

            InferenceRequest inferenceRequest = buildInferenceRequestWithHistory(query, contexts, businessSessionId);
            Flux<ApiChatChunk> stream = inferenceService.streamInfer(inferenceRequest);

            processStreamResponse(stream, userDestination, businessSessionId, contexts);

        } catch (Exception e) {
            log.error("流式搜索初始化失败", e);
            sendErrorResponse(userDestination, "初始化失败: " + e.getMessage());
        }
    }

    private boolean isChitChatQuery(String query) {
        String queryType = determineQueryType(query);
        return "闲聊".equals(queryType);
    }

    private List<VectorSearchResult> searchContexts(String query) {
        List<VectorSearchResult> contexts = ragVectorService.search(query);
        log.debug("向量检索完成，返回结果数量: {}", contexts.size());
        return contexts;
    }

    private void processStreamResponse(Flux<ApiChatChunk> stream, String userDestination, String businessSessionId, List<VectorSearchResult> contexts) {
        StringBuilder fullResponse = new StringBuilder();
        stream.subscribe(
                chunk -> handleStreamChunk(chunk, fullResponse, userDestination),
                error -> handleStreamError(error, userDestination),
                () -> handleStreamComplete(fullResponse, userDestination, businessSessionId, contexts)
        );
    }

    private void handleStreamChunk(ApiChatChunk chunk, StringBuilder fullResponse, String userDestination) {
        if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            return;
        }
        
        ApiChatChunk.Choice choice = chunk.getChoices().get(0);
        if (choice.getDelta() == null || choice.getDelta().getContent() == null) {
            return;
        }
        
        String content = choice.getDelta().getContent();
        fullResponse.append(content);
        
        StreamResponse response = StreamResponse.builder()
                .content(content)
                .finishReason(null)
                .message(null)
                .build();
        RagWebSocketHandler.sendMessageToSession(userDestination, response);
    }

    private void handleStreamError(Throwable error, String userDestination) {
        log.error("流式推理发生错误", error);
        sendErrorResponse(userDestination, "推理过程发生错误: " + error.getMessage());
    }

    private void handleStreamComplete(StringBuilder fullResponse, String userDestination, String businessSessionId, List<VectorSearchResult> contexts) {
        String finalResponse = fullResponse.toString();
        log.info("流式推理完成，总响应长度: {}", finalResponse.length());
        
        chatSessionService.addAssistantMessage(businessSessionId, finalResponse);
        
        StreamResponse completeResponse = StreamResponse.builder()
                .content("")
                .finishReason("stop")
                .message(null)
                .references(contexts)
                .build();
        RagWebSocketHandler.sendMessageToSession(userDestination, completeResponse);
    }

    private void sendErrorResponse(String userDestination, String message) {
        StreamResponse errorResponse = StreamResponse.builder()
                .content("")
                .finishReason(null)
                .message(message)
                .build();
        RagWebSocketHandler.sendMessageToSession(userDestination, errorResponse);
    }

    /**
     * 构造包含历史消息的推理请求
     */
    private InferenceRequest buildInferenceRequestWithHistory(String query, List<VectorSearchResult> contexts, String sessionId) {
        // 获取历史消息
        List<InferenceRequest.Message> historyMessages = chatSessionService.getSessionHistory(sessionId);

        // 构建当前查询的请求
        InferenceRequest currentRequest = promptConstructionService.buildInferenceRequest(query, contexts);

        // 合并历史消息和当前消息
        List<InferenceRequest.Message> allMessages = new ArrayList<>(historyMessages);
        allMessages.addAll(currentRequest.getMessages());

        // 创建新的请求对象
        InferenceRequest requestWithHistory = new InferenceRequest();
        requestWithHistory.setMessages(allMessages);

        log.debug("构建包含历史的请求，历史消息数: {}, 总消息数: {}", historyMessages.size(), allMessages.size());

        return requestWithHistory;
    }

    // 意图判断逻辑
    private String determineQueryType(String query) {
        String intentPrompt = promptConstructionService.buildIntentDetectionPrompt();

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
