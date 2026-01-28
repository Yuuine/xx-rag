package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.api.AppApi;
import yuuine.xxrag.app.application.service.ChatSessionService;
import yuuine.xxrag.dto.common.Result;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class AppApiImpl implements AppApi {

    private final FileUploadProcessingService fileUploadProcessingService;
    private final DocumentManagementService documentManagementService;
    private final SearchInferenceService searchInferenceService;
    private final ChatSessionService chatSessionService;

    @Override
    public Result<Object> uploadFiles(List<MultipartFile> files) {
        return fileUploadProcessingService.uploadFiles(files);
    }

    @Override
    public Result<Object> getDocList() {
        return documentManagementService.getDocList();
    }

    @Override
    public Result<Object> deleteDocuments(List<String> fileMd5s) {
        return documentManagementService.deleteDocuments(fileMd5s);
    }

    @Override
    public void streamSearch(String query, String userDestination) {
        searchInferenceService.streamSearch(query, userDestination);
    }

    @Override
    public Result<Object> deleteSession(String sessionId) {
        try {
            chatSessionService.deleteSession(sessionId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除会话失败: {}", sessionId, e);
            return Result.error("删除会话失败");
        }
    }

    @Override
    public Result<Object> deleteSessionBefore(String sessionId, java.time.LocalDateTime beforeDate) {
        try {
            if (beforeDate == null) {
                // 如果没有提供 beforeDate，则删除整个会话
                chatSessionService.deleteSession(sessionId);
            } else {
                // 删除指定会话中在 beforeDate 之前的历史记录
                chatSessionService.deleteSessionBefore(sessionId, beforeDate);
            }
            return Result.success();
        } catch (Exception e) {
            log.error("按日期删除会话历史失败: {}", sessionId, e);
            return Result.error("按日期删除会话历史失败");
        }
    }

    @Override
    public Result<Object> deleteAllSessions() {
        try {
            chatSessionService.deleteAllSessions();
            return Result.success();
        } catch (Exception e) {
            log.error("删除所有会话失败", e);
            return Result.error("删除所有会话失败");
        }
    }

}