package yuuine.xxrag.app.api;

import org.springframework.modulith.NamedInterface;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.common.Result;

import java.util.List;
import java.time.LocalDateTime;

@NamedInterface("app-api")
public interface AppApi {

    /**
     * 处理文件上传和存储的完整流程
     */
    Result<Object> uploadFiles(List<MultipartFile> files);

    /**
     * 获取文档列表
     */
    Result<Object> getDocList();

    /**
     * 删除文档
     */
    Result<Object> deleteDocuments(List<String> fileMd5s);

    /**
     * WebSocket流式搜索
     */
    void streamSearch(String query, String userDestination);

    /**
     * 删除某个会话及其所有历史（数据库 & 内存缓存）
     */
    Result<Object> deleteSession(String sessionId);

    /**
     * 删除某个会话在指定日期之前的消息
     */
    Result<Object> deleteSessionBefore(String sessionId, LocalDateTime beforeDate);

    /**
     * 删除所有会话历史
     */
    Result<Object> deleteAllSessions();

    /**
     * 删除所有会话中在 beforeDate 之前的历史（全局清理）
     */
    Result<Object> deleteAllSessionsBefore(LocalDateTime beforeDate);
}