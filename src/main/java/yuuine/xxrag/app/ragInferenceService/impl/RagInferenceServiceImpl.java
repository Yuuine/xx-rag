package yuuine.xxrag.app.ragInferenceService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.dto.request.VectorSearchRequest;
import yuuine.xxrag.inference.api.InferenceService;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.app.config.RagPromptProperties;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.app.api.dto.response.RagInferenceResponse;
import yuuine.xxrag.exception.BusinessException;
import yuuine.xxrag.app.ragInferenceService.RagInferenceService;
import yuuine.xxrag.dto.common.VectorSearchResult;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class RagInferenceServiceImpl implements RagInferenceService {

    private final InferenceService inferenceService;
    private final RagPromptProperties ragPromptProperties;

    @Override
    public RagInferenceResponse inference(VectorSearchRequest appRequest,
                                          List<VectorSearchResult> vectorSearchResults) {
        log.debug("开始推理，查询: {}", appRequest.getQuery());
        log.info("推理请求，查询: {}", appRequest.getQuery());
        log.debug("向量搜索结果数量: {}", vectorSearchResults.size());

        try {
            // 构建上下文
            String context = buildContext(vectorSearchResults);
            log.debug("上下文构建完成，长度: {}", context.length());

            // 从配置中获取系统提示词
            String systemPrompt = ragPromptProperties.getSystemPrompt();
            log.debug("系统提示词长度: {}", systemPrompt.length());

            // 构建用户提示词
            String userPrompt = buildUserPrompt(context, appRequest.getQuery());
            log.debug("用户提示词长度: {}", userPrompt.length());

            // 组合最终提示词 - 将系统提示词和用户提示词合并
            String combinedPrompt = systemPrompt + "\n\n" + userPrompt;
            log.debug("组合提示词长度: {}", combinedPrompt.length());

            // 构造发送给推理服务的请求 - 使用合并后的提示词
            InferenceRequest inferenceReq = buildInferenceRequest(combinedPrompt);
            log.debug("推理请求构建完成");

            // 调用推理服务 - 直接调用服务方法
            log.info("调用推理服务，查询: {}", appRequest.getQuery());
            InferenceResponse inferenceResponse = inferenceService.infer(inferenceReq);
            log.info("推理服务调用完成，查询: {}", appRequest.getQuery());

            // 封装返回结果
            RagInferenceResponse result = buildResponse(appRequest, inferenceResponse, vectorSearchResults);
            log.debug("推理完成，答案长度: {}", result.getAnswer().length());

            return result;
        } catch (Exception e) {
            log.error("推理服务调用失败，查询: {}", appRequest.getQuery(), e);
            throw new BusinessException("推理服务调用失败: " + e.getMessage(), e);
        }
    }

    private String buildContext(List<VectorSearchResult> vectorSearchResults) {
        if (vectorSearchResults == null || vectorSearchResults.isEmpty()) {
            return "";
        }

        return vectorSearchResults.stream()
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))  // 按分数降序
                .map(result -> "来源：" + result.getSource() +
                        "（块索引：" + result.getChunkIndex() + "）\n" +
                        result.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    private String buildUserPrompt(String context, String query) {
        return """
                相关文档内容：
                %s
                
                用户问题：%s
                """.formatted(context.isEmpty() ? "（无相关文档）" : context, query);
    }

    private InferenceRequest buildInferenceRequest(String prompt) {
        InferenceRequest inferenceReq = new InferenceRequest();
        inferenceReq.setQuery(prompt); // 使用合并后的提示词作为查询
        return inferenceReq;
    }

    private RagInferenceResponse buildResponse(VectorSearchRequest appRequest,
                                               InferenceResponse inferenceResponse,
                                               List<VectorSearchResult> vectorSearchResults) {
        RagInferenceResponse response = new RagInferenceResponse();
        response.setQuery(appRequest.getQuery());
        response.setAnswer(inferenceResponse.getAnswer());

        // 构建引用信息
        List<RagInferenceResponse.Reference> references = buildReferences(vectorSearchResults);
        response.setReferences(references);

        return response;
    }

    private List<RagInferenceResponse.Reference> buildReferences(List<VectorSearchResult> vectorSearchResults) {
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
