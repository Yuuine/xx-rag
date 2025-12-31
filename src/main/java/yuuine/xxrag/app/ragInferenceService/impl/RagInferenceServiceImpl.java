package yuuine.xxrag.app.ragInferenceService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import yuuine.xxrag.app.api.dto.response.RagInferenceResponse;
import yuuine.xxrag.app.config.RagPromptProperties;
import yuuine.xxrag.app.exception.BusinessException;
import yuuine.xxrag.app.ragInferenceService.RagInferenceService;
import yuuine.xxrag.dto.common.StreamResult;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.dto.request.VectorSearchRequest;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.inference.api.InferenceService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class RagInferenceServiceImpl implements RagInferenceService {

    private final InferenceService inferenceService;
    private final RagPromptProperties ragPromptProperties;

    /* ====================== 非流式推理 ====================== */
    @Override
    public RagInferenceResponse inference(VectorSearchRequest appRequest,
                                          List<VectorSearchResult> vectorSearchResults) {

        log.info("开始非流式推理，query={}", appRequest.getQuery());

        try {
            String combinedPrompt = buildCombinedPrompt(appRequest, vectorSearchResults);
            InferenceRequest inferenceReq = buildInferenceRequest(combinedPrompt);

            InferenceResponse inferenceResponse = inferenceService.infer(inferenceReq);

            return buildResponse(appRequest, inferenceResponse, vectorSearchResults);

        } catch (Exception e) {
            log.error("非流式推理失败，query={}", appRequest.getQuery(), e);
            throw new BusinessException("推理服务调用失败: " + e.getMessage(), e);
        }
    }

    /* ====================== 流式推理 ====================== */
    @Override
    public Flux<StreamResult<Object>> inferenceStream(VectorSearchRequest appRequest,
                                                      List<VectorSearchResult> vectorSearchResults) {

        log.info("开始流式推理，query={}", appRequest.getQuery());

        try {
            String combinedPrompt = buildCombinedPrompt(appRequest, vectorSearchResults);
            InferenceRequest inferenceReq = buildInferenceRequest(combinedPrompt);

            return inferenceService.inferStream(inferenceReq)
                    .map(token -> StreamResult.<Object>delta(token))
                    .concatWith(Flux.just(StreamResult.done()))
                    .onErrorResume(e -> {
                        log.error("流式推理异常，query={}", appRequest.getQuery(), e);
                        return Flux.just(StreamResult.error("推理失败：" + e.getMessage()));
                    })
                    .doOnSubscribe(s -> log.info("流式推理开始，query={}", appRequest.getQuery()))
                    .doOnComplete(() -> log.info("流式推理结束，query={}", appRequest.getQuery()));

        } catch (Exception e) {
            log.error("构建流式推理请求失败，query={}", appRequest.getQuery(), e);
            return Flux.just(StreamResult.error("流式推理构建失败：" + e.getMessage()));
        }
    }

    /* ====================== 内部工具方法 ====================== */
    private String buildCombinedPrompt(VectorSearchRequest appRequest,
                                       List<VectorSearchResult> vectorSearchResults) {
        String context = buildContext(vectorSearchResults);
        String systemPrompt = ragPromptProperties.getSystemPrompt();
        String userPrompt = buildUserPrompt(context, appRequest.getQuery());
        return systemPrompt + "\n\n" + userPrompt;
    }

    private String buildContext(List<VectorSearchResult> vectorSearchResults) {
        if (vectorSearchResults == null || vectorSearchResults.isEmpty()) {
            return "";
        }
        return vectorSearchResults.stream()
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .map(r -> "来源：" + r.getSource()
                        + "（块索引：" + r.getChunkIndex() + "）\n"
                        + r.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    private String buildUserPrompt(String context, String query) {
        return """
                相关文档内容：
                %s
                
                用户问题：%s
                """.formatted(
                context.isEmpty() ? "（无相关文档）" : context,
                query
        );
    }

    private InferenceRequest buildInferenceRequest(String prompt) {
        InferenceRequest req = new InferenceRequest();
        req.setQuery(prompt);
        return req;
    }

    private RagInferenceResponse buildResponse(VectorSearchRequest appRequest,
                                               InferenceResponse inferenceResponse,
                                               List<VectorSearchResult> vectorSearchResults) {

        RagInferenceResponse response = new RagInferenceResponse();
        response.setQuery(appRequest.getQuery());
        response.setAnswer(inferenceResponse.getAnswer());
        response.setReferences(buildReferences(vectorSearchResults));
        return response;
    }

    private List<RagInferenceResponse.Reference> buildReferences(
            List<VectorSearchResult> vectorSearchResults) {

        if (vectorSearchResults == null || vectorSearchResults.isEmpty()) {
            return List.of();
        }

        return vectorSearchResults.stream()
                .map(v -> {
                    RagInferenceResponse.Reference ref = new RagInferenceResponse.Reference();
                    ref.setChunkId(v.getChunkId());
                    ref.setSource(v.getSource());
                    ref.setChunkIndex(v.getChunkIndex());
                    ref.setContent(v.getContent());
                    ref.setScore(v.getScore());
                    return ref;
                })
                .collect(Collectors.toList());
    }
}
