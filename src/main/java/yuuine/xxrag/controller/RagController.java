package yuuine.xxrag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.common.Result;
import yuuine.xxrag.app.api.AppApi;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@RestController
@RequestMapping("/xx")
@Slf4j
public class RagController {

    private final AppApi appApi;

    @PostMapping("/upload")
    public Result<Object> upload(
            @RequestParam("files") List<MultipartFile> files
    ) {
        return appApi.uploadFiles(files);
    }

    @GetMapping("/getDoc")
    public Result<Object> getDoc() {
        return appApi.getDocList();
    }

    @PostMapping("/delete")
    public Result<Object> deleteDocuments(
            @RequestBody List<String> fileMd5s
    ) {
        return appApi.deleteDocuments(fileMd5s);
    }

    @PostMapping("/search")
    public CompletableFuture<Result<Object>> search(
            @RequestBody String query
    ) {
        return appApi.asyncSearch(query);
    }


    // 基于 Websocket 的流式对话请求
    @MessageMapping("streamSearch")
    public void handleStreamSearch(
            @Payload String query,
            StompHeaderAccessor headerAccessor
    ) {
        String sessionId = headerAccessor.getSessionId();
        String userDestination = "/user/" + sessionId + "/queue/reply";

        System.out.println("收到消息: " + query);

        appApi.streamSearch(query, userDestination);
    }

}