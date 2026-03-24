package yuuine.xxrag.app.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String fileMd5;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime createdAt;
    private Integer pageCount;
    private Long charCount;
    private Integer chunkCount;
}
