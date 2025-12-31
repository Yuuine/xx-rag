package yuuine.xxrag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.Result;
import yuuine.xxrag.VectorSearchRequest;
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
}