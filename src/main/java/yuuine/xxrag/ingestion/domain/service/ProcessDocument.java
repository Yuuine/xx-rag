package yuuine.xxrag.ingestion.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.common.constant.FileConstants;
import yuuine.xxrag.common.util.Md5Util;
import yuuine.xxrag.common.util.MimeTypeDetectorUtil;
import yuuine.xxrag.ingestion.domain.model.DocumentProcessingContext;
import yuuine.xxrag.exception.IngestionBusinessException;
import yuuine.xxrag.exception.ErrorCode;

import java.io.IOException;

@RequiredArgsConstructor
@Service
public class ProcessDocument {

    public DocumentProcessingContext processDocument(MultipartFile file) {

        String fileName = file.getOriginalFilename();

        validateFile(file);

        byte[] fileBytes = readFileBytes(file);

        String md5 = Md5Util.computeMd5(fileBytes);

        String mimeType = MimeTypeDetectorUtil.detectMimeType(fileName, fileBytes);

        DocumentProcessingContext context = new DocumentProcessingContext();
        context.setFileMd5(md5);
        context.setFileName(fileName);
        context.setMimeType(mimeType);
        context.setFileBytes(fileBytes);

        return context;
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IngestionBusinessException(ErrorCode.FILE_GET_BYTES_ERROR, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IngestionBusinessException(ErrorCode.FILE_UPLOAD_EMPTY);
        }
        if (file.getSize() > FileConstants.MAX_FILE_SIZE) {
            throw new IngestionBusinessException(ErrorCode.FILE_SIZE_TOO_LARGE);
        }
    }

}
