package yuuine.xxrag.app.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.api.AppApi;
import yuuine.xxrag.dto.common.Result;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class AppApiImpl implements AppApi {

    private final FileUploadProcessingService fileUploadProcessingService;
    private final DocumentManagementService documentManagementService;
    private final SearchInferenceService searchInferenceService;

    @Override
    public Result<Object> uploadFiles(List<MultipartFile> files) {
        return fileUploadProcessingService.uploadFiles(files);
    }

    @Override
    public Result<Object> getDocList() {
        return documentManagementService.getDocList();
    }

    @Override
    public Result<Object> deleteDocuments(List<String> fileMd5s) {
        return documentManagementService.deleteDocuments(fileMd5s);
    }

    @Override
    public void streamSearch(String query, String userDestination) {
        searchInferenceService.streamSearch(query, userDestination);
    }
}