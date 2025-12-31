package yuuine.xxrag.vector.api;

import org.springframework.modulith.NamedInterface;
import yuuine.xxrag.dto.request.VectorAddRequest;
import yuuine.xxrag.dto.request.VectorSearchRequest;
import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.common.VectorSearchResult;

import java.io.IOException;
import java.util.List;

@NamedInterface("vector-api")
public interface VectorApi {

    /**
     * 批量添加向量（其他模块直接调用此方法）
     */
    VectorAddResult addVectors(List<VectorAddRequest> chunks);

    /**
     * 向量搜索（其他模块直接调用）
     */
    List<VectorSearchResult> search(VectorSearchRequest request) throws IOException;

    /**
     * 根据文件 MD5 批量删除向量（其他模块直接调用）
     */
    void deleteByFileMd5s(List<String> fileMd5s);
}