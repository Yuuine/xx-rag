package yuuine.xxrag.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    FILE_PARSE_ERROR(1000, "文档解析错误"),
    FILE_PARSE_FAILED(1001, "文档解析失败"),
    FILE_UPLOAD_EMPTY(1002, "文件上传为空"),
    FILE_SIZE_TOO_LARGE(1003, "文件大小超出限制"),
    FILE_GET_BYTES_ERROR(1004, "获取文件字节错误"),
    FILE_IO_PROCESS_ERROR(1005, "文件IO处理错误"),
    FILE_UPLOAD_FAILED(1006, "文件上传失败"),
    UNSUPPORTED_FILE_TYPE(1007, "不支持的文件类型"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}