package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.app.application.service.RagInferenceService;
import yuuine.xxrag.inference.api.InferenceService;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.app.config.RagPromptProperties;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.app.application.dto.response.RagInferenceResponse;
import yuuine.xxrag.exception.BusinessException;
import yuuine.xxrag.dto.common.VectorSearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class RagInferenceServiceImpl implements RagInferenceService {

    private final InferenceService inferenceService;
    private final RagPromptProperties ragPromptProperties;

    @Override
    public RagInferenceResponse inference(String userQuery,
                                          List<VectorSearchResult> vectorSearchResults) {

        // 如果没有向量检索结果，认为是闲聊类型，直接将问题发送给API
        if (vectorSearchResults == null || vectorSearchResults.isEmpty()) {
            InferenceRequest inferenceReq = buildInferenceRequest(userQuery, ragPromptProperties.getOrdinarySystemPrompt());
            InferenceResponse inferenceResponse = inferenceService.infer(inferenceReq);

            // 构建闲聊类型的响应
            RagInferenceResponse response = new RagInferenceResponse();

            response.setQuery(userQuery);
            response.setAnswer(inferenceResponse.getAnswer());
            response.setReferences(List.of()); // 闲聊类型没有引用文档

            return response;
        }

        // 如果有向量检索结果，按知识查询逻辑处理
        log.debug("开始推理，查询: {}", userQuery);
        log.info("推理请求，查询: {}", userQuery);
        log.debug("向量搜索结果数量: {}", vectorSearchResults.size());

        try {
            // 构建上下文
            String context = buildContext(vectorSearchResults);
            log.debug("上下文构建完成，长度: {}", context.length());

            String systemPrompt = ragPromptProperties.getKnowledgeSystemPrompt();
            log.debug("系统提示词长度: {}", systemPrompt.length());

            // 构建用户提示词
            String userPrompt = buildUserPrompt(context, userQuery);
            log.debug("用户提示词长度: {}", userPrompt.length());

            // 构造发送给推理服务的请求 - 使用合并后的提示词
            InferenceRequest inferenceReq = buildInferenceRequest(userPrompt, ragPromptProperties.getKnowledgeSystemPrompt());
            log.debug("推理请求构建完成");

            // 调用推理服务 - 直接调用服务方法
            log.info("调用推理服务，查询: {}", userQuery);
            InferenceResponse inferenceResponse = inferenceService.infer(inferenceReq);
            log.info("推理服务调用完成，查询问题: {}", userQuery);

            // 封装返回结果
            RagInferenceResponse result = buildResponse(userQuery, inferenceResponse, vectorSearchResults);
            log.info("推理完成");

            return result;
        } catch (Exception e) {
            log.error("推理服务调用失败，查询: {}", userQuery, e);
            throw new BusinessException("推理服务调用失败: " + e.getMessage(), e);
        }
    }

    private String buildContext(List<VectorSearchResult> vectorSearchResults) {
        if (vectorSearchResults == null || vectorSearchResults.isEmpty()) {
            return "";
        }

        return vectorSearchResults.stream()
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))  // 按分数降序
                .filter(result -> result.getContent() != null && !result.getContent().trim().isEmpty()) // 过滤掉内容为空的结果
                .map(result ->
                        "来源：" + "\n" +
                                "文档名称：" + result.getSource() + "\n" +
                                "文档内容：" + "\n" + result.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    private String buildUserPrompt(String context, String query) {
        return """
                相关文档内容：
                %s
                
                用户问题：%s
                """.formatted(context.isEmpty() ? "（无相关文档）" : context, query);
    }

    private InferenceRequest buildInferenceRequest(String userMessage, String systemPrompt) {
        InferenceRequest inferenceReq = new InferenceRequest();
        List<InferenceRequest.Message> messages = new ArrayList<>();
        messages.add(new InferenceRequest.Message("system", systemPrompt));
        messages.add(new InferenceRequest.Message("user", userMessage));

        inferenceReq.setMessages(messages);
        return inferenceReq;
    }

    private RagInferenceResponse buildResponse(String userQuery,
                                               InferenceResponse inferenceResponse,
                                               List<VectorSearchResult> vectorSearchResults) {
        RagInferenceResponse response = new RagInferenceResponse();

        response.setAnswer(inferenceResponse.getAnswer());
        response.setQuery(userQuery);

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