package yuuine.xxrag.ingestion.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.xxrag.ingestion.domain.models.DocumentProcessingContext;
import yuuine.xxrag.ingestion.domain.parser.DocumentParser;
import yuuine.xxrag.ingestion.domain.parser.DocumentParserRegistry;

/**
 * 查找解析器
 * 处理"找不到解析器"的情况
 * 调用实际业务逻辑解析文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParserService {

    private final DocumentParserRegistry registry;

    /**
     * 解析文件内容
     *
     * @param context 文件处理上下文，包含待解析文件的信息
     * @return 解析后的字符串结果
     * @throws UnsupportedOperationException 当不支持的文件类型时抛出异常
     */
    public String parse(DocumentProcessingContext context) {

        // 获取文件的MIME类型
        String mimeType = context.getMimeType();
        log.info("[FileParser] 开始路由解析器，文件类型: {}", mimeType);

        // 根据MIME类型查找对应的文档解析器
        DocumentParser parser = registry.getParser(mimeType);

        // 检查是否找到合适的解析器
        if (parser == null) {
            log.error("[FileParser] 不支持的文件类型: {}", mimeType);
            throw new UnsupportedOperationException("不支持的文件类型: " + mimeType);
        }

        log.info("[FileParser] 命中解析器: {}", parser.getClass().getSimpleName());

        // 调用解析器解析文件并返回结果
        return parser.parse(context);
    }
}

