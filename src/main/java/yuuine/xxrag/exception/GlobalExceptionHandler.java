package yuuine.xxrag.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yuuine.xxrag.dto.common.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({BusinessException.class, IngestionBusinessException.class})
    @ResponseStatus(HttpStatus.OK)
    public Result<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        log.warn("业务异常 traceId={}, uri={}, code={}, msg={}",
                traceId, request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.OK)
    public Result<Object> handleValidationException(Exception e, HttpServletRequest request) {
        Map<String, String> errors = collectValidationErrors(e);
        String firstError = errors.values().stream().findFirst().orElse(ErrorCode.BAD_REQUEST.getMessage());
        log.warn("参数校验失败 traceId={}, uri={}, errors={}", MDC.get("traceId"), request.getRequestURI(), errors);
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), firstError);
    }

    private Map<String, String> collectValidationErrors(Exception e) {
        Map<String, String> errors = new HashMap<>();
        List<FieldError> fieldErrors;
        
        if (e instanceof MethodArgumentNotValidException validException) {
            fieldErrors = validException.getBindingResult().getFieldErrors();
        } else if (e instanceof BindException bindException) {
            fieldErrors = bindException.getFieldErrors();
        } else {
            return errors;
        }
        
        fieldErrors.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数 traceId={}, uri={}, msg={}", MDC.get("traceId"), request.getRequestURI(), e.getMessage());
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Object> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 traceId={}, uri={}", MDC.get("traceId"), request.getRequestURI(), e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }

    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Void> handleClientAbortException(ClientAbortException e, HttpServletRequest request) {
        log.debug("客户端中断连接 - URL: {}, Method: {}",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("未捕获异常 traceId={}, uri={}", MDC.get("traceId"), request.getRequestURI(), e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统异常，请稍后重试");
    }
}