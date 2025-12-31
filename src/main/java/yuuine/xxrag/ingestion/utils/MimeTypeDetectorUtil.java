package yuuine.xxrag.ingestion.utils;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import yuuine.xxrag.ingestion.exception.BusinessException;
import yuuine.xxrag.ingestion.exception.ErrorCode;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Set;

public final class MimeTypeDetectorUtil {

    private static final DefaultDetector DETECTOR = new DefaultDetector();

    /**
     * 允许的 MIME 白名单
     */
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown"
    );

    private MimeTypeDetectorUtil() {
    }

    /**
     * MIME 探测（优先基于文件扩展名，宽松模式）
     */
    public static String detectMimeType(String fileName, byte[] fileBytes) {

        if (fileBytes == null || fileBytes.length == 0) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE, "文件内容为空");
        }

        // 1. 优先基于扩展名推断 MIME 类型
        String mimeType = getMimeFromExtension(fileName);

        // 如果推断成功且在白名单中，直接返回（宽松检测）
        if (mimeType != null && SUPPORTED_MIME_TYPES.contains(mimeType)) {
            return mimeType;
        }

        // 2. Fallback 到 Tika DefaultDetector（如果扩展名未匹配）
        try (ByteArrayInputStream in = new ByteArrayInputStream(fileBytes)) {
            MediaType mediaType = DETECTOR.detect(in, new Metadata());
            mimeType = normalizeMimeType(mediaType.toString());
        } catch (Exception e) {
            mimeType = "application/octet-stream";
        }

        // 3. 白名单校验
        if (!SUPPORTED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(
                    ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "不支持的文件类型: " + mimeType + "（文件名: " + fileName + "）"
            );
        }

        return mimeType;
    }

    /**
     * 根据文件扩展名推断 MIME 类型
     */
    private static String getMimeFromExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        String lowerName = fileName.toLowerCase(Locale.ROOT);

        if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
            return "text/markdown";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        }

        return null;
    }

    /**
     * MIME normalization
     */
    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "application/octet-stream";
        }
        int idx = mimeType.indexOf(';');
        return idx > 0 ? mimeType.substring(0, idx).trim() : mimeType;
    }
}