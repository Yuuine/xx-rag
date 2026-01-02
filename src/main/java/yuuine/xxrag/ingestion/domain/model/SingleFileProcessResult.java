package yuuine.xxrag.ingestion.domain.model;

import lombok.Getter;

import java.util.List;

@Getter
public class SingleFileProcessResult {

    private String filename;
    private String fileMd5;
    private boolean success;
    private String errorMessage;
    private List<Chunk> chunks;

    public static SingleFileProcessResult success(
            String filename,
            String fileMd5,
            List<Chunk> chunks
    ) {
        SingleFileProcessResult r = new SingleFileProcessResult();
        r.filename = filename;
        r.fileMd5 = fileMd5;
        r.success = true;
        r.chunks = chunks;
        return r;
    }

    public static SingleFileProcessResult failure(
            String filename,
            String errorMessage
    ) {
        SingleFileProcessResult r = new SingleFileProcessResult();
        r.filename = filename;
        r.success = false;
        r.errorMessage = errorMessage;
        return r;
    }

}
