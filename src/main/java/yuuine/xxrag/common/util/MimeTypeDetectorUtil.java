package yuuine.xxrag.common.util;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import yuuine.xxrag.common.constant.FileConstants;
import yuuine.xxrag.exception.ErrorCode;
import yuuine.xxrag.exception.IngestionBusinessException;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public final class MimeTypeDetectorUtil {

    private static final DefaultDetector DETECTOR = new DefaultDetector();

    private MimeTypeDetectorUtil() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    public static String detectMimeType(String fileName, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IngestionBusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE, "文件内容为空");
        }

        String mimeType = getMimeFromExtension(fileName);

        if (mimeType != null && FileConstants.ALLOWED_FILE_TYPES.contains(mimeType)) {
            return mimeType;
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(fileBytes)) {
            MediaType mediaType = DETECTOR.detect(in, new Metadata());
            mimeType = normalizeMimeType(mediaType.toString());
        } catch (Exception e) {
            mimeType = "application/octet-stream";
        }

        if (!FileConstants.ALLOWED_FILE_TYPES.contains(mimeType)) {
            throw new IngestionBusinessException(
                    ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "不支持的文件类型: " + mimeType + "（文件名: " + fileName + "）"
            );
        }

        return mimeType;
    }

    private static String getMimeFromExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        String lowerName = fileName.toLowerCase(Locale.ROOT);

        if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lowerName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
            return "text/markdown";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        }

        return null;
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "application/octet-stream";
        }
        int idx = mimeType.indexOf(';');
        return idx > 0 ? mimeType.substring(0, idx).trim() : mimeType;
    }
}
