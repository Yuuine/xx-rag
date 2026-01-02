package yuuine.xxrag.app.ragInferenceService.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.dto.request.VectorSearchRequest;
import yuuine.xxrag.inference.api.InferenceService;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.app.config.RagPromptProperties;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.app.dto.reponse.RagInferenceResponse;
import yuuine.xxrag.exception.BusinessException;
import yuuine.xxrag.app.ragInferenceService.RagInferenceService;
import yuuine.xxrag.dto.common.VectorSearchResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        log.debug("开始判断查询问题类型");

        String query = appRequest.getQuery();

        // 判断查询类型：知识查询或闲聊
        String queryType = determineQueryType(query);
        log.info("查询类型判断结果: {}，直接调用推理服务", queryType);

        // 如果是闲聊类型，直接将问题发送给API
        if ("闲聊".equals(queryType)) {
            InferenceRequest inferenceReq = buildInferenceRequest(query);
            InferenceResponse inferenceResponse = inferenceService.infer(inferenceReq);

            // 构建闲聊类型的响应
            RagInferenceResponse response = new RagInferenceResponse();

            String newQuery = """
                    你是潇潇知识问答助手，需遵守：
                    1. 仅用简体中文作答。
                    
                    我的问题：%s
                    """.formatted(query);

            response.setQuery(newQuery);
            System.out.println(newQuery);
            response.setAnswer(inferenceResponse.getAnswer());
            response.setReferences(List.of()); // 闲聊类型没有引用文档

            return response;
        }

        // 如果是知识查询类型，按原有逻辑处理
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
            log.info("推理服务调用完成，查询问题: {}", appRequest.getQuery());

            // 封装返回结果
            RagInferenceResponse result = buildResponse(appRequest, inferenceResponse, vectorSearchResults);
            log.info("推理完成");

            return result;
        } catch (Exception e) {
            log.error("推理服务调用失败，查询: {}", appRequest.getQuery(), e);
            throw new BusinessException("推理服务调用失败: " + e.getMessage(), e);
        }
    }

    private String determineQueryType(String query) {
        // 构建用于意图判断的提示词
        String intentPrompt = """
                请判断以下用户输入属于哪一类意图：
                知识查询：用户希望获取事实、操作步骤、定义、解释等具体信息。
                闲聊：用户进行问候、情感表达、无明确信息需求的对话。
                用户输入：%s
                你只需返回"知识查询"或者"闲聊"，不要返回除了这两个词语以外的任何内容。
                """.formatted(query);

        InferenceRequest intentRequest = new InferenceRequest();
        intentRequest.setQuery(intentPrompt);

        try {
            // 调用推理服务判断意图
            InferenceResponse intentResponse = inferenceService.infer(intentRequest);
            String result = intentResponse.getAnswer().trim();

            // 返回判断结果，只返回"知识查询"或"闲聊"
            if (result.contains("闲聊")) {
                return "闲聊";
            } else {
                return "知识查询";
            }
        } catch (Exception e) {
            log.warn("意图判断失败，将默认按知识查询处理: {}", e.getMessage());
            return "知识查询"; // 默认按知识查询处理
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

    private InferenceRequest buildInferenceRequest(String prompt) {
        InferenceRequest inferenceReq = new InferenceRequest();
        System.out.println(prompt);
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
