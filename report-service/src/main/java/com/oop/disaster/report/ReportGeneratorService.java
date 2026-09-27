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

    public byte[] generatePdf(ReportRequest request) throws IOException {

        List<String> columns = request.getColumns();
        List<Map<String, Object>> rows = request.getRows();

        try (PDDocument document = new PDDocument()) {

            PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float margin = 50;
            float pageHeight = PDRectangle.A4.getHeight();
            float rowHeight = 18;

            int rowIndex = 0;
            boolean firstPage = true;

            while (firstPage || rowIndex < rows.size()) {

                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                PDPageContentStream cs = new PDPageContentStream(document, page);

                float y = pageHeight - margin;

                if (firstPage) {
                    cs.beginText();
                    cs.setFont(titleFont, 16);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(request.getTitle() == null ? "Report" : request.getTitle());
                    cs.endText();
                    y -= 28;
                    firstPage = false;
                }

                cs.beginText();
                cs.setFont(headerFont, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText(String.join("   |   ", columns));
                cs.endText();
                y -= rowHeight;

                int maxRowsThisPage = (int) ((y - margin) / rowHeight);

                if (maxRowsThisPage > 0 && rowIndex < rows.size()) {

                    cs.beginText();
                    cs.setFont(bodyFont, 9);
                    cs.newLineAtOffset(margin, y);

                    int rowsThisPage = 0;

                    while (rowIndex < rows.size() && rowsThisPage < maxRowsThisPage) {

                        Map<String, Object> row = rows.get(rowIndex);

                        StringBuilder line = new StringBuilder();
                        for (String col : columns) {
                            Object value = row.get(col);
                            line.append(value == null ? "-" : value.toString()).append("   |   ");
                        }

                        cs.showText(line.toString());
                        cs.newLineAtOffset(0, -rowHeight);

                        rowIndex++;
                        rowsThisPage++;
                    }

                    cs.endText();
                }

                cs.close();

                if (rows.isEmpty()) {
                    break;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
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