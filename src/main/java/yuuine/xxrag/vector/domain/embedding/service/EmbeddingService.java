package yuuine.xxrag.vector.domain.embedding.service;

import yuuine.xxrag.vector.domain.embedding.model.ResponseResult;
import yuuine.xxrag.vector.dto.request.VectorAddRequest;

import java.util.List;

public interface EmbeddingService {

    // 批量向量化 ( add 服务）
    ResponseResult embedBatch(List<VectorAddRequest>  chunks);

    // 单条 query 向量化 ( search 服务）
    float[] embedQuery(String query);
}
