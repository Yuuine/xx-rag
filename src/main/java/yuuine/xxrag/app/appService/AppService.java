package yuuine.xxrag.app.appService;

import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.dto.common.Result;
import yuuine.xxrag.app.dto.request.InferenceRequest;

import java.util.List;

public interface AppService {

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
     * 搜索并推理
     */
    Result<Object> search(InferenceRequest query);
}