package yuuine.xxrag.exception;

import lombok.Getter;

@Getter
public class IngestionBusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String details;

    // 1. 基础构造：仅 ErrorCode
    public IngestionBusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    // 2. 带详细信息 ErrorCode + 详细信息
    public IngestionBusinessException(ErrorCode errorCode, String details) {
        super(errorCode.getMessage() + (details != null ? ": " + details : ""));
        this.errorCode = errorCode;
        this.details = details;
    }

    // 3. 带异常原因（用于包装底层异常）
    public IngestionBusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    // 4. 详细信息 + 异常原因
    public IngestionBusinessException(ErrorCode errorCode, String details, Throwable cause) {
        super(errorCode.getMessage() + (details != null ? ": " + details : ""), cause);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    // 提供getCode方法以兼容统一的异常处理器
    public int getCode() {
        return this.errorCode.getCode();
    }
}