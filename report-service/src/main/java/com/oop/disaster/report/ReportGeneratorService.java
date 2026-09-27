package com.oop.disaster.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class ReportGeneratorService {

    // ---------- PDF ----------

    private static final float PDF_MARGIN = 36;
    private static final float PDF_FONT_SIZE = 8;
    private static final float PDF_ROW_HEIGHT = 16;
    private static final float PDF_CELL_PADDING = 4;

    /**
     * Landscape A4 table: title and generation time on the first page, a
     * header row repeated on every page, column widths sized to the content,
     * long values shortened with "..." so nothing runs off the page, and page
     * numbers in the footer.
     */
    public byte[] generatePdf(ReportRequest request) throws IOException {

        List<String> columns = request.getColumns() == null ? List.of() : request.getColumns();
        List<Map<String, Object>> rows = request.getRows() == null ? List.of() : request.getRows();
        String title = pdfSafe(request.getTitle() == null ? "Report" : request.getTitle());

        PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        float tableWidth = pageSize.getWidth() - 2 * PDF_MARGIN;
        float[] widths = columnWidths(columns, rows, headerFont, bodyFont, tableWidth);

        try (PDDocument document = new PDDocument()) {

            int rowIndex = 0;
            int pageNumber = 0;

            do {
                pageNumber++;
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    float y = pageSize.getHeight() - PDF_MARGIN;

                    if (pageNumber == 1) {
                        text(cs, titleFont, 16, PDF_MARGIN, y - 12, title);
                        text(cs, bodyFont, 9, PDF_MARGIN, y - 28,
                                "Generated " + java.time.LocalDateTime.now().withNano(0).toString().replace('T', ' ')
                                        + "   |   " + rows.size() + " record(s)");
                        y -= 44;
                    }

                    // Header row
                    y = drawRow(cs, headerFont, columns, widths, y, true);

                    // Body rows until the page is full
                    while (rowIndex < rows.size() && y - PDF_ROW_HEIGHT > PDF_MARGIN + 14) {
                        Map<String, Object> row = rows.get(rowIndex);
                        List<String> cells = new java.util.ArrayList<>();
                        for (String col : columns) {
                            Object value = row.get(col);
                            cells.add(value == null ? "" : value.toString());
                        }
                        y = drawRow(cs, bodyFont, cells, widths, y, false);
                        rowIndex++;
                    }

                    if (rows.isEmpty()) {
                        text(cs, bodyFont, 9, PDF_MARGIN, y - 14, "No records match the selected filters.");
                    }

                    text(cs, bodyFont, 8, pageSize.getWidth() - PDF_MARGIN - 40, PDF_MARGIN - 14,
                            "Page " + pageNumber);
                }
            } while (rowIndex < rows.size());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /** Draws one table row (grey background for the header) and returns the y below it. */
    private float drawRow(PDPageContentStream cs, PDFont font, List<String> cells, float[] widths,
                          float y, boolean header) throws IOException {
        float x = PDF_MARGIN;
        float bottom = y - PDF_ROW_HEIGHT;

        if (header) {
            cs.setNonStrokingColor(0.88f, 0.91f, 0.93f);
            cs.addRect(PDF_MARGIN, bottom, sum(widths), PDF_ROW_HEIGHT);
            cs.fill();
            cs.setNonStrokingColor(0f, 0f, 0f);
        }

        cs.setStrokingColor(0.75f, 0.75f, 0.75f);
        cs.setLineWidth(0.5f);
        for (int i = 0; i < widths.length; i++) {
            cs.addRect(x, bottom, widths[i], PDF_ROW_HEIGHT);
            cs.stroke();
            String value = i < cells.size() ? cells.get(i) : "";
            String fitted = fit(pdfSafe(value), font, widths[i] - 2 * PDF_CELL_PADDING);
            text(cs, font, PDF_FONT_SIZE, x + PDF_CELL_PADDING, bottom + 5, fitted);
            x += widths[i];
        }
        return bottom;
    }

    /** Widths proportional to each column's longest value (capped), scaled to fill the table. */
    private float[] columnWidths(List<String> columns, List<Map<String, Object>> rows,
                                 PDFont headerFont, PDFont bodyFont, float tableWidth) throws IOException {
        float[] widths = new float[columns.size()];
        float total = 0;
        for (int i = 0; i < columns.size(); i++) {
            float w = textWidth(headerFont, pdfSafe(columns.get(i)));
            for (Map<String, Object> row : rows) {
                Object v = row.get(columns.get(i));
                if (v != null) {
                    w = Math.max(w, textWidth(bodyFont, pdfSafe(v.toString())));
                }
            }
            widths[i] = Math.min(Math.max(w, 30), 260) + 2 * PDF_CELL_PADDING;
            total += widths[i];
        }
        float scale = total == 0 ? 1 : tableWidth / total;
        for (int i = 0; i < widths.length; i++) {
            widths[i] *= scale;
        }
        return widths;
    }

    private static void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String value)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(value);
        cs.endText();
    }

    private static float textWidth(PDFont font, String value) throws IOException {
        return font.getStringWidth(value) / 1000 * PDF_FONT_SIZE;
    }

    /** Shortens a value with "..." until it fits the available width. */
    private static String fit(String value, PDFont font, float maxWidth) throws IOException {
        if (textWidth(font, value) <= maxWidth) {
            return value;
        }
        String cut = value;
        while (!cut.isEmpty() && textWidth(font, cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.isEmpty() ? "" : cut + "...";
    }

    /** The standard PDF fonts only cover Latin-1; replace anything else so the report never fails. */
    static String pdfSafe(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\u2013', '\u2014' -> out.append('-');
                case '\u2018', '\u2019' -> out.append('\'');
                case '\u201C', '\u201D' -> out.append('"');
                case '\n', '\r', '\t' -> out.append(' ');
                default -> out.append((c >= 0x20 && c <= 0x7E) || (c >= 0xA0 && c <= 0xFF) ? c : '?');
            }
        }
        return out.toString();
    }

    private static float sum(float[] values) {
        float total = 0;
        for (float v : values) {
            total += v;
        }
        return total;
    }

    // ---------- DOCX ----------

    public byte[] generateDocx(ReportRequest request) throws IOException {

        List<String> columns = request.getColumns();
        List<Map<String, Object>> rows = request.getRows();

        try (XWPFDocument doc = new XWPFDocument()) {

            XWPFParagraph titlePara = doc.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(request.getTitle() == null ? "Report" : request.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            doc.createParagraph();

            int totalRows = Math.max(rows.size() + 1, 1);
            int totalCols = Math.max(columns.size(), 1);

            XWPFTable table = doc.createTable(totalRows, totalCols);

            for (int c = 0; c < columns.size(); c++) {
                XWPFTableCell cell = table.getRow(0).getCell(c);
                cell.removeParagraph(0);
                XWPFParagraph p = cell.addParagraph();
                XWPFRun r = p.createRun();
                r.setText(columns.get(c));
                r.setBold(true);
            }

            for (int r = 0; r < rows.size(); r++) {
                Map<String, Object> row = rows.get(r);
                for (int c = 0; c < columns.size(); c++) {
                    Object value = row.get(columns.get(c));
                    table.getRow(r + 1).getCell(c).setText(value == null ? "" : value.toString());
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    // ---------- XLSX ----------

    public byte[] generateXlsx(ReportRequest request) throws IOException {

        List<String> columns = request.getColumns();
        List<Map<String, Object>> rows = request.getRows();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            String sheetName = request.getTitle() == null
                    ? "Report"
                    : request.getTitle().replaceAll("[\\\\/*?\\[\\]:]", "").trim();

            if (sheetName.isEmpty()) {
                sheetName = "Report";
            }
            if (sheetName.length() > 31) {
                sheetName = sheetName.substring(0, 31);
            }

            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < columns.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(columns.get(c));
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < rows.size(); r++) {
                Map<String, Object> rowData = rows.get(r);
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < columns.size(); c++) {
                    Object value = rowData.get(columns.get(c));
                    Cell cell = row.createCell(c);
                    cell.setCellValue(value == null ? "" : value.toString());
                }
            }

            for (int c = 0; c < columns.size(); c++) {
                sheet.autoSizeColumn(c);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ---------- CSV ----------

    public byte[] generateCsv(ReportRequest request) {

        List<String> columns = request.getColumns();
        List<Map<String, Object>> rows = request.getRows();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < columns.size(); i++) {
            sb.append(escapeCsv(columns.get(i)));
            if (i < columns.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("\n");

        for (Map<String, Object> row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                Object value = row.get(columns.get(i));
                sb.append(escapeCsv(value == null ? "" : value.toString()));
                if (i < columns.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}