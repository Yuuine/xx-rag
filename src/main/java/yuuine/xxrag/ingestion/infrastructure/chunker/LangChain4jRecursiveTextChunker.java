package yuuine.xxrag.ingestion.infrastructure.chunker;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.common.util.UUIDUtil;
import yuuine.xxrag.ingestion.infrastructure.config.ChunkerProperties;
import yuuine.xxrag.ingestion.domain.model.Chunk;
import yuuine.xxrag.ingestion.domain.service.TextChunker;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
public class LangChain4jRecursiveTextChunker implements TextChunker {

    private final ChunkerProperties properties;

    public LangChain4jRecursiveTextChunker(ChunkerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void chunkStream(String text, Consumer<Chunk> chunkConsumer) {
        if (text == null || text.isBlank()) {
            log.debug("Input text is null or blank, skipping chunking.");
            return;
        }

        Document document = Document.from(text);
        var splitter = DocumentSplitters.recursive(properties.getChunkSize(), properties.getOverlap());
        List<TextSegment> segments = splitter.split(document);

        int index = 0;
        for (TextSegment segment : segments) {
            String chunkText = segment.text().trim();

            if (chunkText.isEmpty()) {
                continue;
            }

            Chunk chunk = new Chunk();
            chunk.setChunkId(UUIDUtil.uuidGenerate());
            chunk.setChunkIndex(index++);
            chunk.setChunkText(chunkText);
            chunk.setCharCount(chunkText.codePointCount(0, chunkText.length()));

            chunkConsumer.accept(chunk);
        }

        log.debug("Finished chunking text of length {} into {} chunks (chunkSize={}, overlap={}).",
                text.length(), index, properties.getChunkSize(), properties.getOverlap());
    }
}