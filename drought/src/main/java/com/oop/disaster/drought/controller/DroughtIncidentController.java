package com.oop.disaster.drought.controller;

import com.oop.disaster.drought.entity.DroughtAuditTrail;
import com.oop.disaster.drought.entity.DroughtIncident;
import com.oop.disaster.drought.security.JwtService;
import com.oop.disaster.drought.service.DroughtIncidentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.oop.disaster.drought.service.AlertClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Drought incident REST API.
 *
 * Role rules (enforced here, never only in the UI):
 *  - RECORDER   : creates incidents for their own ward only; sees approved
 *                 incidents plus their own submissions; edits/deletes only
 *                 their own, not-yet-approved records.
 *  - SUPERVISOR : (drought only - checked by JwtAuthFilter) sees all drought
 *                 records and approves / rejects / requests corrections.
 *  - ADMIN      : provincial administrator - read only, may see pending records.
 *  - NATIONAL   : read only, approved records only.
 * The acting user written to the audit trail always comes from the signed JWT.
 */
@RestController
@RequestMapping("/api/drought/incidents")
public class DroughtIncidentController {

    private final DroughtIncidentService service;
    private final JwtService jwtService;
    private final AlertClient alertClient;

    public DroughtIncidentController(DroughtIncidentService service, JwtService jwtService,
                                     AlertClient alertClient) {
        this.service = service;
        this.jwtService = jwtService;
        this.alertClient = alertClient;
    }

    record Caller(String username, String role, String ward) {
        boolean is(String r) { return r.equalsIgnoreCase(role); }
    }

    Caller caller(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return new Caller(null, null, null);
        }
        String token = header.substring(7);
        return new Caller(
                jwtService.extractUsername(token),
                jwtService.extractRole(token),
                jwtService.extractWard(token));
    }

    private static ResponseEntity<Map<String, String>> forbidden(String message) {
        return ResponseEntity.status(403).body(Map.of("error", "Forbidden: " + message));
    }

    private boolean isVisible(DroughtIncident incident, Caller c) {
        if ("APPROVED".equals(incident.getStatus())) {
            return true;
        }
        if (c.role() == null) {
            return false;
        }
        if (c.is("SUPERVISOR") || c.is("ADMIN")) {
            return true;
        }
        if (c.is("RECORDER")) {
            return c.username() != null && c.username().equalsIgnoreCase(incident.getReporter());
        }
        return false; // NATIONAL: approved only
    }

    private ResponseEntity<Map<String, String>> checkRecorderCanModify(DroughtIncident existing, Caller c) {
        if (c.ward() != null && existing.getWard() != null
                && !c.ward().equalsIgnoreCase(existing.getWard())) {
            return forbidden("this incident belongs to a different ward");
        }
        if (c.username() == null || !c.username().equalsIgnoreCase(existing.getReporter())) {
            return forbidden("you can only modify incidents you captured");
        }
        if ("APPROVED".equals(existing.getStatus())) {
            return forbidden("approved incidents can no longer be modified");
        }
        return null;
    }

    @PreAuthorize("hasAnyRole('RECORDER','SUPERVISOR','ADMIN','NATIONAL')")
    @GetMapping
    public List<DroughtIncident> getAll(HttpServletRequest request) {
        Caller c = caller(request);
        return service.getAllIncidents().stream().filter(i -> isVisible(i, c)).toList();
    }

    @PreAuthorize("hasAnyRole('RECORDER','SUPERVISOR','ADMIN','NATIONAL')")
    @GetMapping("/approved")
    public List<DroughtIncident> getApproved() {
        return service.getAllIncidents().stream().filter(i -> "APPROVED".equals(i.getStatus())).toList();
    }

    @PreAuthorize("hasAnyRole('RECORDER','SUPERVISOR','ADMIN','NATIONAL')")
    @GetMapping("/{id}")
    public ResponseEntity<DroughtIncident> getById(@PathVariable Long id, HttpServletRequest request) {
        Optional<DroughtIncident> found = service.getIncidentById(id);
        if (found.isEmpty() || !isVisible(found.get(), caller(request))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(found.get());
    }

    @PreAuthorize("hasAnyRole('RECORDER','SUPERVISOR','ADMIN','NATIONAL')")
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<DroughtAuditTrail>> getAudit(@PathVariable Long id, HttpServletRequest request) {
        Optional<DroughtIncident> found = service.getIncidentById(id);
        if (found.isPresent() && !isVisible(found.get(), caller(request))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.getAuditTrail(id));
    }

    /** Sends the incident to the alert-service, which decides whether it meets the alert criteria. */
    private DroughtIncident raiseAlert(DroughtIncident i) {
        Map<String, Object> indicators = new LinkedHashMap<>();
        indicators.put("rainfallDeficitMm", i.getRainfallDeficitMm());
        indicators.put("consecutiveDryDays", i.getConsecutiveDryDays());
        indicators.put("cropFailurePercentage", i.getCropFailurePercentage());
        indicators.put("peopleFacingWaterShortages", i.getPeopleFacingWaterShortages());
        indicators.put("livestockMortalityCount", i.getLivestockMortalityCount());
        alertClient.notifyIncident("DROUGHT", i.getId(), i.getWard(), i.getDistrict(), i.getProvince(),
                i.getSeverity(), i.getLatitude(), i.getLongitude(), indicators);
        return i;
    }

    @PreAuthorize("hasRole('RECORDER')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DroughtIncident incident, HttpServletRequest request) {
        Caller c = caller(request);

        if (c.ward() != null) {
            if (incident.getWard() == null || incident.getWard().isBlank()) {
                incident.setWard(c.ward());
            } else if (!c.ward().equalsIgnoreCase(incident.getWard())) {
                return forbidden("you can only capture incidents for your own ward (" + c.ward() + ")");
            }
        }
        incident.setReporter(c.username());

        return ResponseEntity.ok(raiseAlert(service.createIncident(incident)));
    }

    @PreAuthorize("hasRole('RECORDER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody DroughtIncident incident,
                                    HttpServletRequest request) {
        Caller c = caller(request);
        Optional<DroughtIncident> existing = service.getIncidentById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity<Map<String, String>> denied = checkRecorderCanModify(existing.get(), c);
        if (denied != null) {
            return denied;
        }
        if (c.ward() != null && incident.getWard() != null
                && !c.ward().equalsIgnoreCase(incident.getWard())) {
            return forbidden("you can only capture incidents for your own ward (" + c.ward() + ")");
        }

        return service.updateIncident(id, incident, c.username())
                .map(this::raiseAlert)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('RECORDER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Caller c = caller(request);
        Optional<DroughtIncident> existing = service.getIncidentById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity<Map<String, String>> denied = checkRecorderCanModify(existing.get(), c);
        if (denied != null) {
            return denied;
        }

        service.deleteIncident(id, c.username());
        return ResponseEntity.noContent().build();
    }

    // ---------------- Approval workflow (SUPERVISOR only) ----------------
    // Any "performedBy" query parameter sent by older clients is ignored:
    // the actor is always the authenticated user.

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<DroughtIncident> approve(@PathVariable Long id, HttpServletRequest request) {
        return service.approveIncident(id, caller(request).username())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<DroughtIncident> reject(@PathVariable Long id,
                                                  @RequestParam String reason,
                                                  HttpServletRequest request) {
        return service.rejectIncident(id, reason, caller(request).username())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/request-correction")
    public ResponseEntity<DroughtIncident> requestCorrection(@PathVariable Long id,
                                                             @RequestParam String notes,
                                                             HttpServletRequest request) {
        return service.requestCorrection(id, notes, caller(request).username())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
