package yuuine.xxrag.ingestion.infrastructure.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import yuuine.xxrag.exception.ErrorCode;
import yuuine.xxrag.exception.IngestionBusinessException;
import yuuine.xxrag.ingestion.domain.model.DocumentProcessingContext;
import yuuine.xxrag.ingestion.domain.service.DocumentParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ExcelParser implements DocumentParser {

    @Value("${ingestion.excel.max-size:52428800}")
    private long maxFileSize;

    @Value("${ingestion.excel.max-rows-per-sheet:100000}")
    private int maxRowsPerSheet;

    @Value("${ingestion.excel.output-format:markdown}")
    private String outputFormat;

    @Value("${ingestion.excel.evaluate-formulas:true}")
    private boolean evaluateFormulas;

    @Value("${ingestion.excel.sanitize-pii:false}")
    private boolean sanitizePii;

    @Value("${ingestion.excel.text-include-separator:true}")
    private boolean textIncludeSeparator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE); // 更准 PII 正则

    @Override
    public List<String> supportedMimeTypes() {
        return List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel");
    }

    @Override
    public String parse(DocumentProcessingContext context) {
        byte[] fileBytes = context.getFileBytes();
        String fileName = context.getFileName();

        validateInput(fileBytes, fileName);

        log.info("[ExcelParser] 开始解析 Excel 为文本结构: name={}, size={}字节, format={}", fileName, fileBytes.length, outputFormat);
        long startTime = System.currentTimeMillis();

        try (InputStream is = new ByteArrayInputStream(fileBytes);
             Workbook workbook = WorkbookFactory.create(is)) { // 支持 XLS/XLSX

            FormulaEvaluator evaluator = evaluateFormulas ? workbook.getCreationHelper().createFormulaEvaluator() : null;
            DataFormatter formatter = new DataFormatter();

            StringBuilder result = new StringBuilder();
            int totalSheets = workbook.getNumberOfSheets();
            int hiddenSheets = 0;
            // 计算隐藏工作表数量
            for (int i = 0; i < totalSheets; i++) {
                if (workbook.isSheetHidden(i)) {
                    hiddenSheets++;
                }
            }
            for (int i = 0; i < totalSheets; i++) {
                if (workbook.isSheetHidden(i)) continue;

                Sheet sheet = workbook.getSheetAt(i);
                processMergedRegions(sheet, evaluator, formatter); // 增强：类型安全复制

                String sheetContent;
                if ("markdown".equalsIgnoreCase(outputFormat)) {
                    result.append("## Sheet: ").append(sheet.getSheetName()).append("\n\n");
                    sheetContent = convertSheetToMarkdown(sheet, evaluator, formatter);
                } else if ("json".equalsIgnoreCase(outputFormat)) {
                    sheetContent = convertSheetToJson(sheet, evaluator, formatter);
                } else {
                    result.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                    sheetContent = convertSheetToText(sheet, evaluator, formatter);
                }
                result.append(sheetContent).append("\n\n");
            }

            String output = result.toString().trim();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[ExcelParser] 解析完成，处理 {} 个 sheets，文本长度 {} 字符，耗时 {} ms", totalSheets - hiddenSheets, output.length(), duration);

            return output;

        } catch (IOException e) {
            log.error("[ExcelParser] 解析失败: name={}, error={}", fileName, e.getMessage(), e);
            throw new IngestionBusinessException(ErrorCode.FILE_PARSE_ERROR, e);
        }
    }

    private void validateInput(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IngestionBusinessException(ErrorCode.FILE_EMPTY_ERROR, "文件为空");
        }
        if (fileBytes.length > maxFileSize) {
            throw new IngestionBusinessException(ErrorCode.FILE_TOO_LARGE_ERROR, "文件超过最大大小限制");
        }
    }

    private void processMergedRegions(Sheet sheet, FormulaEvaluator evaluator, DataFormatter formatter) {
        for (CellRangeAddress merged : sheet.getMergedRegions()) {
            Row firstRow = sheet.getRow(merged.getFirstRow());
            if (firstRow == null) continue;
            Cell firstCell = firstRow.getCell(merged.getFirstColumn());
            if (firstCell == null) continue;

            // 类型安全复制：使用 formatted 值，并根据类型设置
            String formattedValue = getFormattedCellValue(firstCell, evaluator, formatter);
            for (int r = merged.getFirstRow(); r <= merged.getLastRow(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) row = sheet.createRow(r);
                for (int c = merged.getFirstColumn(); c <= merged.getLastColumn(); c++) {
                    if (r == merged.getFirstRow() && c == merged.getFirstColumn()) continue; // 跳过源 cell
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    setCellValueSafe(cell, formattedValue, firstCell.getCellType());
                }
            }
        }
    }

    private void setCellValueSafe(Cell cell, String value, CellType originalType) {
        try {
            switch (originalType) {
                case NUMERIC -> cell.setCellValue(Double.parseDouble(value));
                case BOOLEAN -> cell.setCellValue(Boolean.parseBoolean(value));
                case FORMULA -> cell.setCellFormula(value); // 如果是公式，保留原样
                default -> cell.setCellValue(value);
            }
        } catch (NumberFormatException e) {
            log.warn("类型转换失败，回退到字符串: {}", value);
            cell.setCellValue(value);
        }
    }

    private String convertSheetToMarkdown(Sheet sheet, FormulaEvaluator evaluator, DataFormatter formatter) {
        return buildSheetContent(sheet, evaluator, formatter, " | ", "---", true);
    }

    private String convertSheetToText(Sheet sheet, FormulaEvaluator evaluator, DataFormatter formatter) {
        return buildSheetContent(sheet, evaluator, formatter, "\t", "-----", textIncludeSeparator);
    }

    private String buildSheetContent(Sheet sheet, FormulaEvaluator evaluator, DataFormatter formatter,
                                     String colDelimiter, String sepFiller, boolean includeSeparator) {
        StringBuilder content = new StringBuilder();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return "";

        // 单 pass 计算 maxCols 和构建内容
        int maxCols = headerRow.getLastCellNum();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) maxCols = Math.max(maxCols, row.getLastCellNum());
        }

        // 构建 header
        StringJoiner header = new StringJoiner(colDelimiter);
        for (int j = 0; j < maxCols; j++) {
            Cell cell = headerRow.getCell(j);
            header.add(getFormattedCellValue(cell, evaluator, formatter));
        }
        content.append(header).append("\n");

        // 可选 separator
        if (includeSeparator) {
            StringJoiner separator = new StringJoiner(colDelimiter);
            for (int j = 0; j < maxCols; j++) {
                separator.add(sepFiller);
            }
            content.append(separator).append("\n");
        }

        // 构建数据行
        int rowCount = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            if (rowCount >= maxRowsPerSheet) {
                log.warn("[ExcelParser] Sheet {} 超过行限制，截断于 {} 行", sheet.getSheetName(), maxRowsPerSheet);
                break;
            }
            Row row = sheet.getRow(i);
            if (row == null) continue;

            StringJoiner data = new StringJoiner(colDelimiter);
            for (int j = 0; j < maxCols; j++) {
                Cell cell = row.getCell(j);
                String value = getFormattedCellValue(cell, evaluator, formatter);
                if (sanitizePii && isPii(value)) value = "[REDACTED]";
                data.add(value);
            }
            content.append(data).append("\n");
            rowCount++;
        }
        return content.toString().trim();
    }

    private String convertSheetToJson(Sheet sheet, FormulaEvaluator evaluator, DataFormatter formatter) {
        ArrayNode jsonArray = objectMapper.createArrayNode();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return jsonArray.toString();

        int maxCols = 0;
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) maxCols = Math.max(maxCols, row.getLastCellNum());
        }

        List<String> headers = new ArrayList<>(maxCols);
        for (int j = 0; j < maxCols; j++) {
            Cell cell = headerRow.getCell(j);
            headers.add(getFormattedCellValue(cell, evaluator, formatter));
        }

        int rowCount = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            if (rowCount >= maxRowsPerSheet) break;
            Row row = sheet.getRow(i);
            if (row == null) continue;
            ObjectNode jsonRow = objectMapper.createObjectNode();
            for (int j = 0; j < maxCols; j++) {
                Cell cell = row.getCell(j);
                String value = getFormattedCellValue(cell, evaluator, formatter);
                if (sanitizePii && isPii(value)) value = "[REDACTED]";
                jsonRow.put(headers.get(j), value);
            }
            jsonArray.add(jsonRow);
            rowCount++;
        }
        return jsonArray.toString();
    }

    private String getFormattedCellValue(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (cell == null) return "";
        if (evaluateFormulas && cell.getCellType() == CellType.FORMULA) {
            try {
                return formatter.formatCellValue(cell, evaluator);
            } catch (Exception e) {
                log.warn("公式求值失败，回退到公式字符串: {}", cell.getCellFormula());
                return cell.getCellFormula();
            }
        }
        return formatter.formatCellValue(cell).trim();
    }

    private boolean isPii(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }
}