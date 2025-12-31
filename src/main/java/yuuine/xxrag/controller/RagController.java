package yuuine.xxrag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import yuuine.xxrag.dto.common.Result;
import yuuine.xxrag.dto.common.StreamResult;
import yuuine.xxrag.dto.request.VectorSearchRequest;
import yuuine.xxrag.app.api.AppService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/xx")
@Slf4j
public class RagController {

    private final AppService appService;  // 注入暴露接口

    @PostMapping("/upload")
    public Result<Object> upload(
            @RequestParam("files") List<MultipartFile> files
    ) {
        return appService.uploadFiles(files);
    }

    @GetMapping("/getDoc")
    public Result<Object> getDoc() {
        return appService.getDocList();
    }

    @PostMapping("/delete")
    public Result<Object> deleteDocuments(
            @RequestBody List<String> fileMd5s
    ) {
        return appService.deleteDocuments(fileMd5s);
    }

    @PostMapping("/search")
    public Result<Object> search(
            @RequestBody VectorSearchRequest query
    ) {
        return appService.search(query);
    }

    @PostMapping(
            value = "/search/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<StreamResult<Object>>> searchStream(
            @RequestBody VectorSearchRequest query) {

        return appService.searchStream(query)
                .doOnNext(event -> System.out.println("Stream event data: " + event)) // 打印每次返回的数据到控制台
                .map(event ->
                        ServerSentEvent.<StreamResult<Object>>builder()
                                .event("message")
                                .data(event)
                                .build()
                );
    }
}