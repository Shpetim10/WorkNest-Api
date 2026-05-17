package com.worknest.features.company.application.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ExcelExportWriter {

    private static final int MAX_SHEET_NAME_LENGTH = 31;
    private static final int MIN_COLUMN_WIDTH = 12 * 256;
    private static final int MAX_COLUMN_WIDTH = 44 * 256;

    public byte[] write(ExportWorkbookData workbookData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(workbookData.sheetName()));
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle bodyStyle = bodyStyle(workbook);

            writeHeader(sheet, workbookData.headers(), headerStyle);
            writeRows(sheet, workbookData, bodyStyle);
            finishSheet(sheet, workbookData);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void writeHeader(Sheet sheet, List<String> headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);
        for (int column = 0; column < headers.size(); column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(headers.get(column));
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRows(Sheet sheet, ExportWorkbookData workbookData, CellStyle bodyStyle) {
        for (int rowIndex = 0; rowIndex < workbookData.rows().size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<Object> values = workbookData.rows().get(rowIndex);
            for (int column = 0; column < workbookData.headers().size(); column++) {
                Cell cell = row.createCell(column);
                Object value = column < values.size() ? values.get(column) : null;
                writeCellValue(cell, value);
                cell.setCellStyle(bodyStyle);
            }
        }
    }

    private void finishSheet(Sheet sheet, ExportWorkbookData workbookData) {
        int columnCount = workbookData.headers().size();
        if (columnCount == 0) {
            return;
        }

        sheet.createFreezePane(0, 1);
        int lastRow = Math.max(0, workbookData.rows().size());
        sheet.setAutoFilter(new CellRangeAddress(0, lastRow, 0, columnCount - 1));
        for (int column = 0; column < columnCount; column++) {
            sheet.setColumnWidth(column, columnWidth(workbookData, column));
        }
    }

    private int columnWidth(ExportWorkbookData workbookData, int column) {
        int maxChars = workbookData.headers().get(column).length();
        for (List<Object> row : workbookData.rows()) {
            Object value = column < row.size() ? row.get(column) : null;
            maxChars = Math.max(maxChars, value == null ? 0 : value.toString().length());
        }
        return Math.max(MIN_COLUMN_WIDTH, Math.min(MAX_COLUMN_WIDTH, (maxChars + 2) * 256));
    }

    private void writeCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof BigInteger integer) {
            cell.setCellValue(integer.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        border(style);
        return style;
    }

    private CellStyle bodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        border(style);
        return style;
    }

    private void border(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private String safeSheetName(String sheetName) {
        String safe = sheetName == null ? "Data" : sheetName.replaceAll("[\\\\/*?:\\[\\]]", " ").trim();
        if (safe.isBlank()) {
            safe = "Data";
        }
        return safe.length() <= MAX_SHEET_NAME_LENGTH ? safe : safe.substring(0, MAX_SHEET_NAME_LENGTH);
    }
}
