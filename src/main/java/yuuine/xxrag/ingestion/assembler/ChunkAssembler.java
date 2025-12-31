package yuuine.xxrag.ingestion.assembler;

import org.springframework.stereotype.Component;
import yuuine.xxrag.ingestion.domain.chunk.Chunk;
import yuuine.xxrag.ingestion.domain.models.SingleFileProcessResult;
import yuuine.xxrag.ingestion.dto.response.ChunkResponse;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChunkAssembler {

    public List<ChunkResponse> toResponses(SingleFileProcessResult result) {
        List<ChunkResponse> responses = new ArrayList<>();

        for (Chunk chunk : result.getChunks()) {
            ChunkResponse r = new ChunkResponse();
            r.setSource(result.getFilename());
            r.setFileMd5(result.getFileMd5());
            r.setChunkId(chunk.getChunkId());
            r.setChunkIndex(chunk.getChunkIndex());
            r.setChunkText(chunk.getChunkText());
            r.setCharCount(chunk.getCharCount());
            responses.add(r);
        }
        return responses;
    }
}
