package yuuine.xxrag.vector.domain.embedding.service.impl;

import com.alibaba.dashscope.embeddings.TextEmbeddingOutput;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yuuine.xxrag.vector.domain.embedding.model.ResponseResult;
import yuuine.xxrag.vector.util.DashScopeEmbeddingUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceImplTest {

    @Mock
    private DashScopeEmbeddingUtil dashScopeEmbeddingUtil;

    @InjectMocks
    private EmbeddingServiceImpl embeddingService;

    @Test
    void testEmbedQuery_ValidQuery() throws Exception {
        // Mock objects
        String query = "test query";
        TextEmbeddingResult result = mock(TextEmbeddingResult.class);
        TextEmbeddingOutput output = mock(TextEmbeddingOutput.class);
        TextEmbeddingResultItem item = mock(TextEmbeddingResultItem.class);
        when(item.getEmbedding()).thenReturn(List.of(0.1, 0.2, 0.3));
        when(output.getEmbeddings()).thenReturn(List.of(item));
        when(result.getOutput()).thenReturn(output);

        // Mock behavior
        when(dashScopeEmbeddingUtil.generateEmbeddingResult(List.of(query))).thenReturn(result);

        // Test first call (should call API)
        float[] embedding1 = embeddingService.embedQuery(query);
        assertNotNull(embedding1);
        assertEquals(3, embedding1.length);
        verify(dashScopeEmbeddingUtil, times(1)).generateEmbeddingResult(List.of(query));

        // Test second call (should use cache)
        float[] embedding2 = embeddingService.embedQuery(query);
        assertNotNull(embedding2);
        assertEquals(3, embedding2.length);
        verify(dashScopeEmbeddingUtil, times(1)).generateEmbeddingResult(List.of(query)); // Should not call again

        // Verify embeddings are the same
        assertArrayEquals(embedding1, embedding2, 0.001f);
    }

    @Test
    void testEmbedQuery_EmptyQuery() {
        // Test with empty query
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            embeddingService.embedQuery("");
        });
        assertEquals("Query text cannot be null or empty", exception.getMessage());
    }

    @Test
    void testEmbedQuery_NullQuery() {
        // Test with null query
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            embeddingService.embedQuery(null);
        });
        assertEquals("Query text cannot be null or empty", exception.getMessage());
    }

    @Test
    void testEmbedBatch_EmptyChunks() {
        // Test with empty chunks
        ResponseResult result = embeddingService.embedBatch(List.of());
        assertNotNull(result);
        assertEquals(0, result.getVectorAddResult().getSuccessChunk());
        assertEquals(0, result.getVectorAddResult().getFailedChunk());
        assertTrue(result.getRagChunkDocuments().isEmpty());
    }

    @Test
    void testEmbedBatch_NullChunks() {
        // Test with null chunks
        ResponseResult result = embeddingService.embedBatch(null);
        assertNotNull(result);
        assertEquals(0, result.getVectorAddResult().getSuccessChunk());
        assertEquals(0, result.getVectorAddResult().getFailedChunk());
        assertTrue(result.getRagChunkDocuments().isEmpty());
    }
}
