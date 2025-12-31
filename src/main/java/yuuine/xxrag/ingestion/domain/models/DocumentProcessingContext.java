package yuuine.xxrag.ingestion.domain.models;

import lombok.Data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Data
public class DocumentProcessingContext {

    private String fileMd5;
    private String fileName;
    private String mimeType;
    private byte[] fileBytes;

    public InputStream getInputStream() {
        return new ByteArrayInputStream(fileBytes);
    }

}
