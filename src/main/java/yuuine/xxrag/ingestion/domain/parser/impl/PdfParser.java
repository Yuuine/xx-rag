package yuuine.xxrag.ingestion.domain.parser.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import yuuine.xxrag.ingestion.domain.models.DocumentProcessingContext;
import yuuine.xxrag.ingestion.domain.parser.DocumentParser;
import yuuine.xxrag.exception.IngestionBusinessException;
import yuuine.xxrag.exception.ErrorCode;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * PDF 文档解析器
 */
@Slf4j
@Component
public class PdfParser implements DocumentParser {

    @Override
    public List<String> supportedMimeTypes() {
        return List.of("application/pdf");
    }

    @Override
    public String parse(DocumentProcessingContext context) {
        byte[] fileBytes = context.getFileBytes();
        String fileName = context.getFileName();
        log.info("[PDFParser] 开始解析 PDF: name={}, size={}字节", fileName, fileBytes.length);

        try (ByteArrayInputStream is = new ByteArrayInputStream(fileBytes)) {
            BodyContentHandler handler = new BodyContentHandler(-1); // 不限制长度
            Metadata metadata = new Metadata();
            ParseContext newContext = new ParseContext();

            // 配置：保证文本顺序
            PDFParserConfig config = new PDFParserConfig();
            config.setSortByPosition(true);
            newContext.set(PDFParserConfig.class, config);

            // 使用专用 PDFParser
            new PDFParser().parse(is, handler, metadata, newContext);

            String text = handler.toString().trim();
            log.info("[PDFParser] PDF 解析完成，文本长度 {} 字符", text.length());

            return text;
        } catch (Exception e) {
            log.error("[PDFParser] PDF 解析失败: name={}, error={}", fileName, e.getMessage());
            throw new IngestionBusinessException(ErrorCode.FILE_PARSE_ERROR, e);
        }
    }
}