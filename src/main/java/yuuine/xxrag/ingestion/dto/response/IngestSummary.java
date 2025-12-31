package yuuine.xxrag.ingestion.dto.response;

import lombok.Data;


@Data
public class IngestSummary {

    private int totalFiles;                     // 本次请求一共上传了多少个文件
    private FileResult fileResult;              // 文件处理结果

}
