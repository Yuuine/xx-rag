package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yuuine.xxrag.app.config.RagPromptProperties;
import yuuine.xxrag.dto.common.VectorSearchResult;
import yuuine.xxrag.dto.request.InferenceRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 提示词构建服务组件
 * 负责构建各种类型的提示词
 */
@Component
@RequiredArgsConstructor
public class PromptConstructionService {

    private final RagPromptProperties ragPromptProperties;

    /**
     * 构造推理请求
     */
    public InferenceRequest buildInferenceRequest(String userMessage, String systemPrompt) {
        InferenceRequest inferenceReq = new InferenceRequest();
        List<InferenceRequest.Message> messages = new ArrayList<>();
        messages.add(new InferenceRequest.Message("system", systemPrompt));
        messages.add(new InferenceRequest.Message("user", userMessage));

        inferenceReq.setMessages(messages);
        return inferenceReq;
    }

    /**
     * 构造推理请求（根据上下文类型）
     */
    public InferenceRequest buildInferenceRequest(String query, List<VectorSearchResult> contexts) {
        // 判断是否为闲聊类型（没有上下文）
        boolean isChitChat = contexts == null || contexts.isEmpty();

        if (isChitChat) {
            // 闲聊类型：只使用普通系统提示词
            String ordinarySystemPrompt = ragPromptProperties.getOrdinarySystemPrompt();

            return buildInferenceRequest(query, ordinarySystemPrompt);
        } else {
            // 知识查询类型：构建上下文和完整的提示词
            String context = buildContext(contexts);
            String userPrompt = buildUserPrompt(context, query);
            String knowledgeSystemPrompt = ragPromptProperties.getKnowledgeSystemPrompt();

            return buildInferenceRequest(userPrompt, knowledgeSystemPrompt);
        }
    }

    /**
     * 构建上下文内容
     */
    public String buildContext(List<VectorSearchResult> vectorSearchResults) {
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

    /**
     * 构建用户提示词
     */
    public String buildUserPrompt(String context, String query) {
        return """
                相关文档内容：
                %s
                
                用户问题：%s
                """.formatted(context.isEmpty() ? "（无相关文档）" : context, query);
    }

    /**
     * 构建意图判断提示词
     */
    public String buildIntentDetectionPrompt() {
        return """
                请判断用户输入属于哪一类意图：
                知识查询：用户希望获取事实、操作步骤、定义、解释等具体信息。
                闲聊：用户进行问候、情感表达、无明确信息需求的对话。
                用户输入会在接下来的 user 消息中给出。
                你只需返回"闲聊"或者"知识查询"，不要返回除了这两个词语以外的任何内容。
                """;
    }
}