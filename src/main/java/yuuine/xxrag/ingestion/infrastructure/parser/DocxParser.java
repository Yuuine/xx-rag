package yuuine.xxrag.ingestion.infrastructure.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;
import yuuine.xxrag.ingestion.domain.model.DocumentProcessingContext;
import yuuine.xxrag.ingestion.domain.service.DocumentParser;
import yuuine.xxrag.exception.IngestionBusinessException;
import yuuine.xxrag.exception.ErrorCode;

import java.io.InputStream;
import java.util.List;

/**
 * DOCX文档解析器
 * 支持解析Microsoft Word 2007及以后版本的.docx格式文件
 */
@Slf4j
@Component
public class DocxParser implements DocumentParser {

    @Override
    public List<String> supportedMimeTypes() {
        return List.of(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
    }

    /**
     * 解析DOCX文件内容并提取纯文本
     *
     * @param context 文件处理上下文，包含待解析文件的信息
     * @return 解析后的纯文本内容
     */
    @Override
    public String parse(DocumentProcessingContext context) {

        String fileName = context.getFileName();
        log.info("[DocxParser] 开始解析 Docx: name={}, size={}字节", fileName, context.getFileBytes().length);

        try (InputStream is = context.getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {

            StringBuilder sb = new StringBuilder();

            // 解析段落内容
            for (XWPFParagraph p : doc.getParagraphs()) {
                append(sb, p.getText());
            }

            // 解析表格内容
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        append(sb, cell.getText());
                    }
                }
            }

            log.info("[DocxParser] Docx 解析完成，文本长度 {} 字节", sb.length());

            return sb.toString();

        } catch (Exception e) {
            log.error("[DocxParser] Docx 解析失败: name={}, error={}", fileName, e.getMessage());
            throw new IngestionBusinessException(ErrorCode.FILE_PARSE_FAILED, e);
        }
    }

    /**
     * 向StringBuilder中追加文本内容
     * 只有当文本不为空且不为空白字符时才追加，并在末尾添加换行符
     *
     * @param sb   StringBuilder对象
     * @param text 待添加的文本
     */
    private void append(StringBuilder sb, String text) {
        if (text != null && !text.isBlank()) {
            sb.append(text).append('\n');
        }
    }
}
