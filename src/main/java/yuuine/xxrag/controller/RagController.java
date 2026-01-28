package yuuine.xxrag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.api.AppApi;
import yuuine.xxrag.dto.common.Result;

import java.util.List;

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
        log.debug("接收到删除文档请求，文件MD5数量: {}", fileMd5s.size());
        return appApi.deleteDocuments(fileMd5s);
    }


}