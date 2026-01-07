package yuuine.xxrag.inference.service.impl;

import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import yuuine.xxrag.inference.config.DeepSeekProperties;
import yuuine.xxrag.inference.dto.request.ChatRequest;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.inference.dto.response.ChatResponse;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.inference.api.InferenceService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InferenceServiceImpl implements InferenceService {

    private final DeepSeekProperties properties;
    private final WebClient webClient;

    // 通过构造函数注入配置并构建带超时的 WebClient
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
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public InferenceResponse infer(InferenceRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new IllegalArgumentException("查询内容不能为空");
        }


        try {
            // 构建 DeepSeek 请求
            // 将 InferenceRequest 转换为 ChatRequest
            List<ChatRequest.Message> messages = new ArrayList<>();
            for (InferenceRequest.Message message : request.getMessages()) {
                messages.add(new ChatRequest.Message(message.getRole(), message.getContent()));
            }
            ChatRequest chatRequest = getChatRequest(messages);

            log.debug(
                    "调用 DeepSeek API，model: {},  temperature: {}, max-tokens: {}, timeout-seconds: {}",
                    properties.getModel(), properties.getTemperature(), properties.getMaxTokens(), properties.getTimeoutSeconds()
            );

            ChatResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(chatRequest)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                log.error("DeepSeek 返回空响应");
                throw new RuntimeException("LLM 返回空结果");
            }

            String answer = response.getChoices().get(0).getMessage().getContent();

            InferenceResponse inferenceResponse = new InferenceResponse();
            inferenceResponse.setAnswer(answer);

            if (response.getUsage() != null) {
                log.info("Token usage - prompt: {}, completion: {}, total: {}",
                        response.getUsage().getPrompt_tokens(),
                        response.getUsage().getCompletion_tokens(),
                        response.getUsage().getTotal_tokens());
            }

            return inferenceResponse;

        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败，query length: {}", request.getMessages().size(), e);
            throw new RuntimeException("LLM 推理服务异常: " + e.getMessage(), e);
        }
    }

    @NotNull
    private ChatRequest getChatRequest(List<ChatRequest.Message> messages) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(properties.getModel());
        chatRequest.setMessages(messages);
        chatRequest.setTemperature(properties.getTemperature());
        chatRequest.setMax_tokens(properties.getMaxTokens());

        return chatRequest;
    }
}