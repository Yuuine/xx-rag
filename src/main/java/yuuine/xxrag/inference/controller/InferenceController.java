package yuuine.xxrag.inference.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import yuuine.xxrag.dto.request.InferenceRequest;
import yuuine.xxrag.dto.response.InferenceResponse;
import yuuine.xxrag.inference.api.InferenceService;
import yuuine.xxrag.inference.config.DeepSeekProperties;

@RestController
@RequestMapping("/inference")
@RequiredArgsConstructor
public class InferenceController {

    private final InferenceService inferenceService;
    private final DeepSeekProperties properties;  // 注入配置决定模式

    /**
     * 单一统一聊天接口
     * 前端始终调用此端点
     * 服务器根据 deepseek.stream-enabled 配置决定返回 JSON 或 SSE 流
     */
    @PostMapping(
            value = "/chat",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE}
    )
    public Object chat(@RequestBody InferenceRequest request) {

        if (properties.isStream()) {
            // 服务器决策：启用流式 → 返回 SSE 流（Flux<String>）
            return inferenceService.inferStream(request);
        } else {
            // 服务器决策：非流式 → 返回完整 JSON
            return inferenceService.infer(request);
        }
    }
}