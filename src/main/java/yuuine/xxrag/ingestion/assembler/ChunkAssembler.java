package yuuine.xxrag.ingestion.assembler;

import org.springframework.stereotype.Component;
import yuuine.xxrag.dto.response.IngestResponse;
import yuuine.xxrag.ingestion.domain.chunk.Chunk;
import yuuine.xxrag.ingestion.domain.models.SingleFileProcessResult;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChunkAssembler {

    public List<IngestResponse.ChunkResponse> toResponses(SingleFileProcessResult result) {
        List<IngestResponse.ChunkResponse> responses = new ArrayList<>();

        for (Chunk chunk : result.getChunks()) {
            IngestResponse.ChunkResponse r = new IngestResponse.ChunkResponse();
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
