package yuuine.xxrag.app.application.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import yuuine.xxrag.app.application.service.DocService;
import yuuine.xxrag.app.application.service.RagIngestService;
import yuuine.xxrag.app.application.service.RagVectorService;
import yuuine.xxrag.dto.common.Result;
import yuuine.xxrag.dto.common.VectorAddResult;
import yuuine.xxrag.dto.response.IngestResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadProcessingServiceTest {

    @Mock
    private RagIngestService ragIngestService;

    @Mock
    private RagVectorService ragVectorService;

    @Mock
    private DocService docService;

    @InjectMocks
    private FileUploadProcessingService fileUploadProcessingService;

    @Test
    void testUploadFiles_EmptyFiles() {
        Result<Object> result = fileUploadProcessingService.uploadFiles(null);
        assertEquals("file not null", result.getMessage());
        assertEquals(1, result.getCode());
    }

    @Test
    void testUploadFiles_ValidFiles() throws Exception {
        // Mock objects
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.isEmpty()).thenReturn(false);

        IngestResponse ingestResponse = new IngestResponse();
        IngestResponse.ChunkResponse chunkResponse = new IngestResponse.ChunkResponse();
        chunkResponse.setChunkId("chunk1");
        chunkResponse.setFileMd5("md51");
        chunkResponse.setSource("test.pdf");
        chunkResponse.setChunkIndex(0);
        chunkResponse.setChunkText("test content");
        chunkResponse.setCharCount(10);
        ingestResponse.setChunks(List.of(chunkResponse));

        VectorAddResult vectorAddResult = new VectorAddResult();
        vectorAddResult.setSuccessChunk(1);
        vectorAddResult.setFailedChunk(0);

        // Mock behavior
        when(ragIngestService.upload(anyList())).thenReturn(ingestResponse);
        when(ragVectorService.add(anyList())).thenReturn(vectorAddResult);

        // Test
        Result<Object> result = fileUploadProcessingService.uploadFiles(List.of(mockFile));

        // Verify
        assertEquals(0, result.getCode());
        verify(ragIngestService, times(1)).upload(anyList());
        verify(ragVectorService, times(1)).add(anyList());
        verify(docService, times(1)).saveDoc(anyString(), anyString());
    }

    @Test
    void testUploadFiles_InvalidFileType() throws Exception {
        // Mock objects
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("application/zip"); // Invalid file type
        when(mockFile.getSize()).thenReturn(1024L);

        // Test
        Result<Object> result = fileUploadProcessingService.uploadFiles(List.of(mockFile));

        // Verify
        assertEquals("不支持的文件类型", result.getMessage());
        assertEquals(1, result.getCode());
    }

    @Test
    void testUploadFiles_FileTooLarge() throws Exception {
        // Mock objects
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getSize()).thenReturn(150 * 1024 * 1024L); // 150MB, which is over the limit

        // Test
        Result<Object> result = fileUploadProcessingService.uploadFiles(List.of(mockFile));

        // Verify
        assertEquals("文件大小超过限制（最大100MB）", result.getMessage());
        assertEquals(1, result.getCode());
    }

    @Test
    void testUploadFiles_EmptyFileContent() throws Exception {
        // Mock objects
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(0L);
        when(mockFile.isEmpty()).thenReturn(true);

        // Test
        Result<Object> result = fileUploadProcessingService.uploadFiles(List.of(mockFile));

        // Verify
        assertEquals("文件内容为空", result.getMessage());
        assertEquals(1, result.getCode());
        verify(ragIngestService, never()).upload(anyList());
    }
}
