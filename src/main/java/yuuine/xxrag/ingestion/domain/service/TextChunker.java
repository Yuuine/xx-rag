package yuuine.xxrag.ingestion.domain.service;

import yuuine.xxrag.ingestion.domain.model.Chunk;

import java.util.function.Consumer;

/**
 * 文本分块器接口。
 * 定义了如何将一段长文本分割成一系列较小的块。
 */
public interface TextChunker {
    /**
     * 流式处理文本，将产生的每个chunk通过consumer回调传出。
     *
     * @param text          待处理的原始文本
     * @param chunkConsumer 处理每个生成的chunk的消费者
     */
    void chunkStream(String text, Consumer<Chunk> chunkConsumer);
}