package yuuine.xxrag.exception;

import lombok.Getter;

@Getter
public class IngestionBusinessException extends BusinessException {

    private final ErrorCode errorCode;
    private final String details;

    // 1. 基础构造：仅 ErrorCode
    public IngestionBusinessException(ErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
        this.details = null;
    }

    // 2. 带详细信息 ErrorCode + 详细信息
    public IngestionBusinessException(ErrorCode errorCode, String details) {
        super(errorCode, errorCode.getMessage() + (details != null ? ": " + details : ""));
        this.errorCode = errorCode;
        this.details = details;
    }

    // 3. 带异常原因（用于包装底层异常）
    public IngestionBusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    // 4. 详细信息 + 异常原因
    public IngestionBusinessException(ErrorCode errorCode, String details, Throwable cause) {
        super(errorCode, errorCode.getMessage() + (details != null ? ": " + details : ""), cause);
        this.errorCode = errorCode;
        this.details = details;
    }
}