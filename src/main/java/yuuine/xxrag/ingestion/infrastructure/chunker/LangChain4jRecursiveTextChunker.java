package yuuine.xxrag.ingestion.infrastructure.chunker;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yuuine.xxrag.ingestion.infrastructure.config.ChunkerProperties;
import yuuine.xxrag.ingestion.domain.model.Chunk;
import yuuine.xxrag.ingestion.domain.service.TextChunker;
import yuuine.xxrag.ingestion.utils.UUIDUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * 使用 LangChain4j 递归分块器实现。
 */
@Slf4j
@Service
public class LangChain4jRecursiveTextChunker implements TextChunker {

    private final ChunkerProperties properties;

    @Autowired
    public LangChain4jRecursiveTextChunker(ChunkerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void chunkStream(String text, Consumer<Chunk> chunkConsumer) {
        if (text == null || text.isBlank()) {
            log.debug("Input text is null or blank, skipping chunking.");
            return;
        }

        // 创建 LangChain4j Document
        Document document = Document.from(text);

        // 使用配置参数构建递归分块器（基于字符数）
        var splitter = DocumentSplitters.recursive(properties.getChunkSize(), properties.getOverlap());

        List<TextSegment> segments = splitter.split(document);

        int index = 0;
        for (TextSegment segment : segments) {
            String chunkText = segment.text().trim();

            // 跳过完全空白的 chunk
            if (chunkText.isEmpty()) {
                continue;
            }

            Chunk chunk = new Chunk();
            chunk.setChunkId(UUIDUtil.UUIDGenerate());
            chunk.setChunkIndex(index++);
            chunk.setChunkText(chunkText);
            chunk.setCharCount(chunkText.codePointCount(0, chunkText.length()));

            chunkConsumer.accept(chunk);
        }

        log.debug("Finished chunking text of length {} into {} chunks (chunkSize={}, overlap={}).",
                text.length(), index, properties.getChunkSize(), properties.getOverlap());
    }
}