package com.oop.disaster.report;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for turning incident data into reports in every format. */
class ReportGenerationTest {

    private final ReportGeneratorService generator = new ReportGeneratorService();

    private static Map<String, Object> feed() {
        return Map.of("incidents", List.of(
                Map.of("hazard", "FLOOD", "occurredAt", "2026-09-14T06:30:00", "ward", "Ward 3",
                        "district", "Rushinga", "province", "Mashonaland Central", "severity", "HIGH",
                        "operationalStatus", "ONGOING", "latitude", -16.7, "longitude", 32.1,
                        "headline", "Peak water 3.4 m, 22 households displaced — Mazowe basin"),
                Map.of("hazard", "MINING", "occurredAt", "2026-08-02T10:00:00", "ward", "Ward 1",
                        "district", "Rushinga", "severity", "CRITICAL", "operationalStatus", "RESCUE_ONGOING",
                        "headline", "Chimanda Gold Claim: 2 fatalities, 5 trapped/injured")));
    }

    @Test
    void feedIsMappedToReportRows() {
        ReportRequest r = IncidentReportController.toReportRequest("CSV", "", "Rushinga", null, null,
                "2026-01-01", "2026-09-30", feed());

        assertEquals(IncidentReportController.COLUMNS, r.getColumns());
        assertEquals(2, r.getRows().size());
        assertEquals("Flood", r.getRows().get(0).get("Hazard"));
        assertEquals("2026-09-14 06:30", r.getRows().get(0).get("Date"));
        assertTrue(r.getTitle().contains("All Hazards") && r.getTitle().contains("Rushinga")
                && r.getTitle().contains("2026-01-01 to 2026-09-30"));
    }

    @Test
    void emptyFeedGivesEmptyReport() {
        ReportRequest r = IncidentReportController.toReportRequest("PDF", "FIRE", null, null, null, null, null,
                Map.of("incidents", List.of()));
        assertTrue(r.getRows().isEmpty());
        assertTrue(r.getTitle().startsWith("DPDMS Fire Report"));
    }

    @Test
    void everyFormatIsGenerated() throws Exception {
        ReportRequest r = IncidentReportController.toReportRequest("PDF", null, null, null, null, null, null, feed());

        byte[] pdf = generator.generatePdf(r);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

        byte[] docx = generator.generateDocx(r);
        byte[] xlsx = generator.generateXlsx(r);
        assertEquals('P', (char) docx[0]); // DOCX and XLSX are ZIP containers ("PK")
        assertEquals('P', (char) xlsx[0]);

        String csv = new String(generator.generateCsv(r), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("Hazard,Date,Ward"));
        assertTrue(csv.contains("\"Peak water 3.4 m, 22 households displaced"), "commas are quoted");
    }

    @Test
    void pdfHandlesManyRowsAndUnsupportedCharacters() throws Exception {
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 120; i++) {
            rows.add(Map.of("Hazard", "Fire", "Summary", "— long text 中 " + "x".repeat(200)));
        }
        ReportRequest r = new ReportRequest();
        r.setTitle("Stress — test");
        r.setColumns(List.of("Hazard", "Summary"));
        r.setRows(rows);
        assertTrue(generator.generatePdf(r).length > 1000);
    }

    @Test
    void pdfSafeReplacesCharactersOutsideLatin1() {
        assertEquals("a - b ' c ?", ReportGeneratorService.pdfSafe("a — b ’ c 中"));
    }
}
