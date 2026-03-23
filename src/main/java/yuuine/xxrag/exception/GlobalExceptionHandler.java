package yuuine.xxrag.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
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
    public Result<Object> handleBusinessException(RuntimeException e) {
        int code;
        String message;
        
        if (e instanceof BusinessException businessException) {
            code = businessException.getCode();
            message = businessException.getMessage();
        } else if (e instanceof IngestionBusinessException ingestionException) {
            code = ingestionException.getCode();
            message = ingestionException.getMessage();
        } else {
            code = 1;
            message = e.getMessage();
        }
        
        log.warn("业务异常 [code={}]: {}", code, message);
        return Result.error(code, message);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleValidationException(Exception e) {
        Map<String, String> errors = collectValidationErrors(e);
        log.warn("参数校验失败: {}", errors);
        return Result.error(400, "参数校验失败");
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
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.error(500, "系统内部错误");
    }

    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Void> handleClientAbortException(ClientAbortException e, HttpServletRequest request) {
        log.debug("客户端中断连接 - URL: {}, Method: {}",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleException(Exception e) {
        log.error("未捕获的异常", e);
        return Result.error(500, "系统异常，请稍后重试");
    }
}