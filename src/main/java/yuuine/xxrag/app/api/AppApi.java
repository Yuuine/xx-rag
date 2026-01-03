package yuuine.xxrag.app.api;

import org.springframework.modulith.NamedInterface;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.dto.common.Result;
import yuuine.xxrag.dto.request.InferenceRequest;

import java.util.List;

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

    /**e
     * 搜索并推理
     */
    Result<Object> search(String query);
}