package yuuine.xxrag.ingestion.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yuuine.xxrag.ingestion.common.Result;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBizException(BusinessException e, HttpServletRequest request) {
        log.warn("business exception: URL={}, method={}, code={}",
                request.getRequestURI(),
                request.getMethod(),
                e.getErrorCode().getCode(),
                e);

        Result<Object> result = Result.error(e.getErrorCode().getCode(), e.getMessage());

        return ResponseEntity.badRequest().body(result);

    }

}

