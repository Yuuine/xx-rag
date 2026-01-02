package yuuine.xxrag.ingestion.api.dto.response;

import lombok.Data;
import yuuine.xxrag.dto.response.IngestResponse;


@Data
public class IngestSummary {

    private int totalFiles;                     // 本次请求一共上传了多少个文件
    private IngestResponse.FileResult fileResult;              // 文件处理结果接口

}
