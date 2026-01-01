package yuuine.xxrag.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定义业务异常
 * 用于 Service 层抛出明确的业务错误，便于全局异常处理器统一处理
 */
@Getter
@AllArgsConstructor
public class BusinessException extends RuntimeException {

    /**
     * 错误码，默认 1 表示业务错误
     */
    private final int code;

    /**
     * 构造器：使用默认错误码 1
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 1;
    }


    /**
     * 构造器：带原因的业务异常
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 1;
    }
}