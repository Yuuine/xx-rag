package yuuine.xxrag.ingestion.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.ingestion.infrastructure.chunker.LangChain4jRecursiveTextChunker;
import yuuine.xxrag.ingestion.domain.model.Chunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunk服务层。
 * 负责调用分块器，并将产生的领域模型Chunk列表封装返回。
 * 不在此处添加文件元信息（如source, md5），那是上游调用者的责任。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkService {

    private final LangChain4jRecursiveTextChunker langChain4jRecursiveTextChunker;

    /**
     * 对给定文本进行分块处理。
     *
     * @param text 待分块的原始文本
     * @return 包含所有分块结果的 {@link Chunk} 对象列表。如果输入为空，则返回空列表。
     */
    public List<Chunk> getChunks(String text) {
        List<Chunk> chunks = new ArrayList<>();
        // 调用分块器，将每个产出的chunk收集到列表中
        langChain4jRecursiveTextChunker.chunkStream(text, chunks::add);
        log.info("[ChunkService] Generated {} chunks from text.", chunks.size());
        return chunks;
    }
}