package yuuine.xxrag.ingestion.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class FileResult {

    private List<String> successfulFiles;
    private List<String> failedFiles;
}
