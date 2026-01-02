package yuuine.xxrag.ingestion.infrastructure.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import yuuine.xxrag.ingestion.domain.model.DocumentProcessingContext;
import yuuine.xxrag.ingestion.domain.service.DocumentParser;
import yuuine.xxrag.exception.IngestionBusinessException;
import yuuine.xxrag.exception.ErrorCode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class MarkdownParser implements DocumentParser {

    @Override
    public List<String> supportedMimeTypes() {
        return List.of("text/markdown", "text/x-markdown");
    }

    @Override
    public String parse(DocumentProcessingContext context) {
        byte[] fileBytes = context.getFileBytes();
        String fileName = context.getFileName();
        log.info("[MarkdownParser] 开始解析 Markdown: name={}, size={}字节", fileName, fileBytes.length);

        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext parseContext = new ParseContext();

            // 使用文本解析器来处理Markdown文件
            TXTParser txtParser = new TXTParser();
            txtParser.parse(is, handler, metadata, parseContext);

            String text = handler.toString().trim();
            log.info("[MarkdownParser] Markdown 解析完成，文本长度 {} 字符", text.length());

            return text;
        } catch (IOException | TikaException e) {
            log.error("[MarkdownParser] Markdown 解析失败: name={}, error={}", fileName, e.getMessage());
            throw new IngestionBusinessException(ErrorCode.FILE_PARSE_ERROR, e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
