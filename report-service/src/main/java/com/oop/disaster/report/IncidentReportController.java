package com.oop.disaster.report;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;

/**
 * Incident reports built from live data, downloadable straight from the API:
 *
 *   GET /api/reports/incidents?format=PDF&hazard=FLOOD&district=Rushinga&from=2026-01-01&to=2026-09-30
 *
 * The data comes from dashboard-service, called with the caller's own JWT, so a
 * report can only ever contain APPROVED incidents from the hazards the caller is
 * allowed to see. Pending records never reach a report.
 */
@RestController
@RequestMapping("/api/reports")
public class IncidentReportController {

    static final List<String> COLUMNS = List.of(
            "Hazard", "Date", "Ward", "District", "Province", "Severity",
            "Status", "Latitude", "Longitude", "Summary");

    private static final Map<String, String> HAZARD_LABELS = Map.of(
            "FLOOD", "Flood", "DROUGHT", "Drought", "FIRE", "Fire",
            "ZOONOTIC", "Zoonotic disease", "MINING", "Mining accident");

    private final ReportController generator;
    private final DiscoveryClient discovery;
    private final String fallbackUrl;
    private final RestClient http = RestClient.create();

    public IncidentReportController(ReportController generator, DiscoveryClient discovery,
                                    @Value("${report.dashboard.fallback-url:http://localhost:8089}") String fallbackUrl) {
        this.generator = generator;
        this.discovery = discovery;
        this.fallbackUrl = fallbackUrl;
    }

    @GetMapping("/incidents")
    public ResponseEntity<?> incidentReport(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "PDF") String format,
            @RequestParam(required = false) String hazard,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) throws IOException {

        String url = UriComponentsBuilder.fromUriString(dashboardUrl() + "/api/dashboard/incidents")
                .queryParamIfPresent("hazard", blankToEmpty(hazard))
                .queryParamIfPresent("ward", blankToEmpty(ward))
                .queryParamIfPresent("district", blankToEmpty(district))
                .queryParamIfPresent("severity", blankToEmpty(severity))
                .queryParamIfPresent("from", blankToEmpty(from))
                .queryParamIfPresent("to", blankToEmpty(to))
                .encode()
                .toUriString();

        Map<String, Object> feed;
        try {
            feed = http.get().uri(url)
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "dashboard-service refused: " + e.getStatusText()));
        } catch (RestClientException e) {
            // Graceful degradation: report generation depends on live data
            return ResponseEntity.status(503).body(Map.of("error", "Incident data is temporarily unavailable: " + e.getMessage()));
        }

        ReportRequest request = toReportRequest(format, hazard, district, ward, severity, from, to, feed);
        return generator.generate(request);
    }

    /** Converts the dashboard feed into the generic title / columns / rows report request. */
    @SuppressWarnings("unchecked")
    static ReportRequest toReportRequest(String format, String hazard, String district, String ward,
                                         String severity, String from, String to, Map<String, Object> feed) {
        List<Map<String, Object>> incidents = feed == null
                ? List.of()
                : (List<Map<String, Object>>) feed.getOrDefault("incidents", List.of());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> i : incidents) {
            Map<String, Object> row = new LinkedHashMap<>();
            String h = String.valueOf(i.get("hazard"));
            String date = i.get("occurredAt") == null ? "" : i.get("occurredAt").toString().replace('T', ' ');
            row.put("Hazard", HAZARD_LABELS.getOrDefault(h, h));
            row.put("Date", date.length() > 16 ? date.substring(0, 16) : date);
            row.put("Ward", orBlank(i.get("ward")));
            row.put("District", orBlank(i.get("district")));
            row.put("Province", orBlank(i.get("province")));
            row.put("Severity", orBlank(i.get("severity")));
            row.put("Status", orBlank(i.get("operationalStatus")));
            row.put("Latitude", orBlank(i.get("latitude")));
            row.put("Longitude", orBlank(i.get("longitude")));
            row.put("Summary", orBlank(i.get("headline")));
            rows.add(row);
        }

        StringBuilder title = new StringBuilder("DPDMS ");
        title.append(hazard == null || hazard.isBlank()
                ? "All Hazards" : HAZARD_LABELS.getOrDefault(hazard.toUpperCase(), hazard));
        title.append(" Report");
        List<String> filters = new ArrayList<>();
        if (district != null && !district.isBlank()) filters.add(district);
        if (ward != null && !ward.isBlank()) filters.add(ward);
        if (severity != null && !severity.isBlank()) filters.add(severity.toUpperCase() + " severity");
        if ((from != null && !from.isBlank()) || (to != null && !to.isBlank())) {
            filters.add((from == null || from.isBlank() ? "..." : from) + " to " + (to == null || to.isBlank() ? "today" : to));
        }
        if (!filters.isEmpty()) {
            title.append(" - ").append(String.join(", ", filters));
        }

        ReportRequest request = new ReportRequest();
        request.setTitle(title.toString());
        request.setFormat(format);
        request.setColumns(COLUMNS);
        request.setRows(rows);
        return request;
    }

    private String dashboardUrl() {
        try {
            List<ServiceInstance> instances = discovery.getInstances("dashboard-service");
            if (!instances.isEmpty()) {
                return instances.get(0).getUri().toString();
            }
        } catch (Exception ignored) {
            // registry unavailable - use the configured URL
        }
        return fallbackUrl;
    }

    private static Optional<String> blankToEmpty(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static Object orBlank(Object value) {
        return value == null ? "" : value;
    }
}
