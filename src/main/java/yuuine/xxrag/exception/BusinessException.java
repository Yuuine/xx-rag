package yuuine.xxrag.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 * 用于 Service 层抛出明确的业务错误，便于全局异常处理器统一处理
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final ErrorCode errorCode;

    /**
     * 构造器：使用默认错误码 1
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
        this.errorCode = ErrorCode.BUSINESS_ERROR;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }


    /**
     * 构造器：带原因的业务异常
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
        this.errorCode = ErrorCode.BUSINESS_ERROR;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }
}