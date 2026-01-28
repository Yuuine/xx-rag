package yuuine.xxrag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.api.AppApi;
import yuuine.xxrag.app.config.AdminProperties;
import yuuine.xxrag.dto.common.Result;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/xx")
@Slf4j
public class RagController {

    private final AppApi appApi;

    private final AdminProperties adminProperties;

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

    @PostMapping("/deleteSession")
    public Result<Object> deleteSession(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        log.debug("请求删除会话: {}", sessionId);
        return appApi.deleteSession(sessionId);
    }

    @PostMapping("/deleteSessionBefore")
    public Result<Object> deleteSessionBefore(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        String before = body.get("beforeDate");
        LocalDateTime beforeDate = null;
        try {
            if (before != null && !before.isEmpty()) {
                beforeDate = LocalDateTime.parse(before);
            }
        } catch (Exception e) {
            log.warn("无法解析 beforeDate: {}", before, e);
            return Result.error("无法解析 beforeDate，使用 ISO_LOCAL_DATE_TIME 格式");
        }
        return appApi.deleteSessionBefore(sessionId, beforeDate);
    }

    @PostMapping("/deleteAllSessions")
    public Result<Object> deleteAllSessions(@RequestBody Map<String, String> body) {
        String pwd = body.get("password");
        String cleanupPassword = adminProperties.getCleanupPassword();
        if (cleanupPassword == null || cleanupPassword.isEmpty()) {
            log.warn("未在配置中设置 cleanup-password，拒绝执行删除所有会话");
            return Result.error("服务器未启用此操作");
        }
        if (pwd == null || !pwd.equals(cleanupPassword)) {
            log.warn("尝试删除所有会话但提供了错误的密码");
            return Result.error("密码错误，拒绝执行");
        }

        log.warn("管理员通过密码验证，开始删除所有会话（危险操作）");
        return appApi.deleteAllSessions();
    }

}
