package yuuine.xxrag.app.application.service.impl;

import jakarta.websocket.Session;
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
import yuuine.xxrag.websocket.RagWebSocketSessionManager;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchInferenceService {

    private final RagVectorService ragVectorService;
    private final InferenceService inferenceService;
    private final PromptConstructionService promptConstructionService;
    private final ChatSessionService chatSessionService;
    private final RagWebSocketSessionManager wsSessionManager;

    public void streamSearch(String query, Session session) {
        executeRagSearchStream(query, session);
    }

    private void executeRagSearchStream(String query, Session session) {
        try {
            log.info("开始流式搜索，查询: {}", query);

            boolean isChitChat = isChitChatQuery(query);
            List<VectorSearchResult> contexts = isChitChat ? null : searchContexts(query);

            InferenceRequest inferenceRequest = buildInferenceRequestWithHistory(query, contexts);
            Flux<ApiChatChunk> stream = inferenceService.streamInfer(inferenceRequest);

            processStreamResponse(stream, session);

        } catch (Exception e) {
            log.error("流式搜索初始化失败", e);
            sendErrorResponse(session, "初始化失败: " + e.getMessage());
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

    private void processStreamResponse(Flux<ApiChatChunk> stream, Session session) {
        StringBuilder fullResponse = new StringBuilder();
        stream.subscribe(
                chunk -> handleStreamChunk(chunk, fullResponse, session),
                error -> handleStreamError(error, session),
                () -> handleStreamComplete(fullResponse, session)
        );
    }

    private void handleStreamChunk(ApiChatChunk chunk, StringBuilder fullResponse, Session session) {
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
        wsSessionManager.sendToSession(session, response);
    }

    private void handleStreamError(Throwable error, Session session) {
        log.error("流式推理发生错误", error);
        sendErrorResponse(session, "推理过程发生错误: " + error.getMessage());
    }

    private void handleStreamComplete(StringBuilder fullResponse, Session session) {
        String finalResponse = fullResponse.toString();
        log.info("流式推理完成，总响应长度: {}", finalResponse.length());

        chatSessionService.addAssistantMessage(finalResponse);

        StreamResponse completeResponse = StreamResponse.builder()
                .content("")
                .finishReason("stop")
                .message(null)
                .build();
        wsSessionManager.sendToSession(session, completeResponse);
    }

    private void sendErrorResponse(Session session, String message) {
        StreamResponse errorResponse = StreamResponse.builder()
                .content("")
                .finishReason(null)
                .message(message)
                .build();
        wsSessionManager.sendToSession(session, errorResponse);
    }

    private InferenceRequest buildInferenceRequestWithHistory(String query, List<VectorSearchResult> contexts) {
        List<InferenceRequest.Message> historyMessages = chatSessionService.getMessages();

        InferenceRequest currentRequest = promptConstructionService.buildInferenceRequest(query, contexts);

        List<InferenceRequest.Message> allMessages = new ArrayList<>(historyMessages);
        allMessages.addAll(currentRequest.getMessages());

        InferenceRequest requestWithHistory = new InferenceRequest();
        requestWithHistory.setMessages(allMessages);

        log.debug("构建包含历史的请求，历史消息数: {}, 总消息数: {}", historyMessages.size(), allMessages.size());

        return requestWithHistory;
    }

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
