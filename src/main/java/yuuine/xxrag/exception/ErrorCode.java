package yuuine.xxrag.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    BUSINESS_ERROR(1000, "业务处理失败"),

    FILE_PARSE_ERROR(1100, "文档解析错误"),
    FILE_PARSE_FAILED(1101, "文档解析失败"),
    FILE_UPLOAD_EMPTY(1102, "文件上传为空"),
    FILE_SIZE_TOO_LARGE(1103, "文件大小超出限制"),
    FILE_GET_BYTES_ERROR(1104, "获取文件字节错误"),
    FILE_IO_PROCESS_ERROR(1105, "文件IO处理错误"),
    FILE_UPLOAD_FAILED(1106, "文件上传失败"),
    UNSUPPORTED_FILE_TYPE(1107, "不支持的文件类型"),
    FILE_EMPTY_ERROR(1108, "文件为空"),
    FILE_TOO_LARGE_ERROR(1109, "文件大小超出限制"),

    VECTOR_DELETE_ASYNC_FAILED(2101, "向量删除异步处理失败"),
    OUTBOX_PAYLOAD_PARSE_ERROR(2102, "Outbox 事件载荷解析失败"),
    OUTBOX_PAYLOAD_SERIALIZE_ERROR(2103, "Outbox 事件载荷序列化失败"),

    INFERENCE_REQUEST_INVALID(3101, "推理请求无效"),
    INFERENCE_SERVICE_ERROR(3102, "推理服务异常")
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}