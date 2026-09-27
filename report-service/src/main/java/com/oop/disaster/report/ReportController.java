package com.oop.disaster.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportGeneratorService generatorService;

    public ReportController(ReportGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@RequestBody ReportRequest request) throws IOException {

        byte[] fileBytes;
        String contentType;
        String extension;

        String format = request.getFormat() == null ? "" : request.getFormat().toUpperCase();

        switch (format) {
            case "PDF":
                fileBytes = generatorService.generatePdf(request);
                contentType = "application/pdf";
                extension = "pdf";
                break;
            case "DOCX":
                fileBytes = generatorService.generateDocx(request);
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = "docx";
                break;
            case "XLSX":
                fileBytes = generatorService.generateXlsx(request);
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = "xlsx";
                break;
            case "CSV":
                fileBytes = generatorService.generateCsv(request);
                contentType = "text/csv";
                extension = "csv";
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        String safeTitle = request.getTitle() == null
                ? "report"
                : request.getTitle().replaceAll("[^a-zA-Z0-9]", "_");

        String filename = safeTitle + "." + extension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileBytes);
    }
}