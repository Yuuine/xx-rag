package yuuine.xxrag.app.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.api.AppService;
import yuuine.xxrag.dto.common.Result;
import yuuine.xxrag.dto.request.VectorSearchRequest;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/app")
@Slf4j
public class AppController {

    private final AppService appService;

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
