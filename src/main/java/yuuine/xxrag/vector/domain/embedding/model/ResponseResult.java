package yuuine.xxrag.vector.domain.embedding.model;

import lombok.Data;
import yuuine.xxrag.vector.domain.es.model.RagChunkDocument;
import yuuine.xxrag.VectorAddResult;

import java.util.List;

@Data
public class ResponseResult {

    private List<RagChunkDocument> ragChunkDocuments;
    private VectorAddResult vectorAddResult;

}
