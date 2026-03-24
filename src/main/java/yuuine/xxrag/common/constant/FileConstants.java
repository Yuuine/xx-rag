package yuuine.xxrag.common.constant;

import java.util.Set;

public class FileConstants {

    private FileConstants() {
        throw new UnsupportedOperationException("常量类不能实例化");
    }

    public static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    public static final int SUCCESS_CODE = 0;
    public static final int ERROR_CODE = 1;
}
