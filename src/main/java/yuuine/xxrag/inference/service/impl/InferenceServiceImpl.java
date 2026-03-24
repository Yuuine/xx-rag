package yuuine.xxrag.inference.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import yuuine.xxrag.common.util.ValidationUtils;
import yuuine.xxrag.dto.common.ApiChatChunk;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.inference.api.InferenceService;
import yuuine.xxrag.inference.config.DeepSeekProperties;
import yuuine.xxrag.inference.dto.request.ChatRequest;
import yuuine.xxrag.inference.dto.response.ChatResponse;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InferenceServiceImpl implements InferenceService {

    private static final String CHAT_COMPLETIONS_URI = "/chat/completions";
    private static final String BEARER_PREFIX = "Bearer ";

    private final DeepSeekProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public InferenceServiceImpl(DeepSeekProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = buildWebClient(properties);
    }

    private static WebClient buildWebClient(DeepSeekProperties props) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(props.getTimeoutSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public InferenceResponse infer(InferenceRequest request) {
        validateRequest(request);

        ChatRequest chatRequest = buildChatRequest(request, false);
        logApiCall("调用 DeepSeek API", chatRequest.isStream());

        try {
            ChatResponse response = executeChatCompletion(chatRequest);
            return buildInferenceResponse(response);
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败，query length: {}", request.getMessages().size(), e);
            throw new RuntimeException("LLM 推理服务异常: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<ApiChatChunk> streamInfer(InferenceRequest request) {
        validateRequest(request);

        ChatRequest chatRequest = buildChatRequest(request, true);
        logApiCall("调用 DeepSeek 流式 API", chatRequest.isStream());

        return webClient.post()
                .uri(CHAT_COMPLETIONS_URI)
                .bodyValue(chatRequest)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .filter(this::isValidSseEvent)
                .mapNotNull(this::parseChatChunk)
                .onErrorResume(this::handleStreamError);
    }

    private void validateRequest(InferenceRequest request) {
        if (request == null || ValidationUtils.isNullOrEmpty(request.getMessages())) {
            throw new IllegalArgumentException("查询内容不能为空");
        }
    }

    private ChatRequest buildChatRequest(InferenceRequest request, boolean stream) {
        List<ChatRequest.Message> messages = request.getMessages().stream()
                .map(m -> new ChatRequest.Message(m.getRole(), m.getContent()))
                .toList();
        return createChatRequest(messages, stream);
    }

    @NotNull
    private ChatRequest createChatRequest(List<ChatRequest.Message> messages, boolean stream) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(properties.getModel());
        chatRequest.setMessages(messages);
        chatRequest.setStream(stream);
        chatRequest.setTemperature(properties.getTemperature());
        chatRequest.setMax_tokens(properties.getMaxTokens());
        return chatRequest;
    }

    private void logApiCall(String action, boolean isStream) {
        log.debug("{}, model: {}, stream: {}", action, properties.getModel(), isStream);
    }

    private ChatResponse executeChatCompletion(ChatRequest chatRequest) {
        ChatResponse response = webClient.post()
                .uri(CHAT_COMPLETIONS_URI)
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

        if (response == null || ValidationUtils.isNullOrEmpty(response.getChoices())) {
            log.error("DeepSeek 返回空响应");
            throw new RuntimeException("LLM 返回空结果");
        }

        return response;
    }

    private InferenceResponse buildInferenceResponse(ChatResponse response) {
        String answer = response.getChoices().get(0).getMessage().getContent();

        if (response.getUsage() != null) {
            log.info("Token usage - prompt: {}, completion: {}, total: {}",
                    response.getUsage().getPrompt_tokens(),
                    response.getUsage().getCompletion_tokens(),
                    response.getUsage().getTotal_tokens());
        }

        InferenceResponse inferenceResponse = new InferenceResponse();
        inferenceResponse.setAnswer(answer);
        return inferenceResponse;
    }

    private boolean isValidSseEvent(ServerSentEvent<String> sse) {
        return sse.data() != null && !"[DONE]".equals(sse.data());
    }

    private ApiChatChunk parseChatChunk(ServerSentEvent<String> sse) {
        try {
            return objectMapper.readValue(sse.data(), ApiChatChunk.class);
        } catch (Exception e) {
            log.warn("解析 ApiChatChunk 失败: {}", sse.data(), e);
            return null;
        }
    }

    private Flux<ApiChatChunk> handleStreamError(Throwable error) {
        log.error("DeepSeek 流式 API 调用失败", error);
        return Flux.error(new RuntimeException("LLM 流式推理异常: " + error.getMessage()));
    }
}
