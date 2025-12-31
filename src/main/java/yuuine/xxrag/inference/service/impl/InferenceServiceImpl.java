package yuuine.xxrag.inference.service.impl;

import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import yuuine.xxrag.inference.config.DeepSeekProperties;
import yuuine.xxrag.inference.dto.request.ChatRequest;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.inference.dto.response.ChatResponse;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.inference.api.InferenceService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InferenceServiceImpl implements InferenceService {

    private final DeepSeekProperties properties;
    private final WebClient webClient;

    public InferenceServiceImpl(DeepSeekProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(properties.getTimeoutSeconds(), TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 非流式推理
     */
    @Override
    public InferenceResponse infer(InferenceRequest request) {
        validateRequest(request);

        String query = request.getQuery().trim();

        try {
            ChatRequest chatRequest = buildChatRequest(query, false); // stream = false

            log.debug("调用 DeepSeek API（非流式），model: {}, prompt length: {} chars",
                    properties.getModel(), query.length());

            ChatResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(chatRequest)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            validateResponse(response);

            String answer = null;
            if (response != null) {
                answer = response.getChoices().get(0).getMessage().getContent();
            }
            if (response != null) {
                logTokenUsage(response);
            }

            InferenceResponse inferenceResponse = new InferenceResponse();
            inferenceResponse.setAnswer(answer);
            return inferenceResponse;

        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败，query length: {}", query.length(), e);
            throw new RuntimeException("LLM 推理服务异常: " + e.getMessage(), e);
        }
    }

    /**
     * 流式推理 - 返回实时文本增量
     */
    @Override
    public Flux<String> inferStream(InferenceRequest request) {
        validateRequest(request);

        String query = request.getQuery().trim();

        try {
            ChatRequest chatRequest = buildChatRequest(query, true); // stream = true

            log.debug("调用 DeepSeek API（流式），model: {}, prompt length: {} chars",
                    properties.getModel(), query.length());

            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(chatRequest)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .concatMap(buffer -> {
                        // 将 DataBuffer 转成 String 并释放
                        String chunk = buffer.toString(StandardCharsets.UTF_8);
                        DataBufferUtils.release(buffer);

                        // 如果是结束标记，返回空字符串（后续 filter 会过滤）
                        if (chunk.contains("data: [DONE]")) {
                            return Flux.empty();
                        }

                        // 提取 content 并返回
                        String content = extractContentFromChunk(chunk);
                        if (content == null || content.isEmpty()) {
                            return Flux.empty();
                        }
                        return Flux.just(content);
                    })
                    .onErrorResume(e -> {
                        log.error("DeepSeek 流式调用异常，query length: {}", query.length(), e);
                        return Flux.just("[ERROR: LLM 流式推理异常 - " + e.getMessage() + "]");
                    })
                    .doOnComplete(() -> log.info("DeepSeek 流式响应结束"));

        } catch (Exception e) {
            log.error("构建 DeepSeek 流式请求失败", e);
            return Flux.error(new RuntimeException("LLM 流式推理服务异常: " + e.getMessage(), e));
        }
    }

    private void validateRequest(InferenceRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("查询内容不能为空");
        }
    }

    private ChatRequest buildChatRequest(String query, boolean stream) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(properties.getModel());
        chatRequest.setTemperature(properties.getTemperature());
        chatRequest.setMax_tokens(properties.getMaxTokens());
        chatRequest.setStream(stream);

        ChatRequest.Message userMessage = new ChatRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent(query);
        chatRequest.setMessages(List.of(userMessage));

        return chatRequest;
    }

    private void validateResponse(ChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            log.error("DeepSeek 返回空响应");
            throw new RuntimeException("LLM 返回空结果");
        }
    }

    private void logTokenUsage(ChatResponse response) {
        if (response.getUsage() != null) {
            log.info("Token usage - prompt: {}, completion: {}, total: {}",
                    response.getUsage().getPrompt_tokens(),
                    response.getUsage().getCompletion_tokens(),
                    response.getUsage().getTotal_tokens());
        }
    }

    /**
     * 从 SSE chunk 中提取 content 字段
     * DeepSeek 返回格式示例：
     * data: {"choices":[{"delta":{"content":"你好"}}]}
     * data: [DONE]
     */
    private String extractContentFromChunk(String chunk) {
        if (chunk == null || chunk.isBlank() || chunk.contains("data: [DONE]")) {
            return null;
        }

        // 移除 "data: " 前缀
        String json = chunk.startsWith("data: ") ? chunk.substring(6).trim() : chunk.trim();
        if (json.isEmpty() || json.equals("[DONE]")) {
            return null;
        }

        try {
            // 简单字符串解析
            int contentIdx = json.indexOf("\"content\":\"");
            if (contentIdx == -1) {
                return null;
            }
            contentIdx += 11;

            int endIdx = json.indexOf("\"", contentIdx);
            if (endIdx == -1) {
                return null;
            }

            String content = json.substring(contentIdx, endIdx);
            // 处理转义字符
            return content.replace("\\n", "\n")
                    .replace("\\/", "/")
                    .replace("\\\"", "\"");
        } catch (Exception e) {
            log.warn("解析 SSE chunk 失败: {}", chunk, e);
            return null;
        }
    }
}