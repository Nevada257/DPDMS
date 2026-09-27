package com.oop.disaster.flood_service.controller;

import com.oop.disaster.flood_service.JwtService;
import com.oop.disaster.flood_service.model.FloodAuditLog;
import com.oop.disaster.flood_service.model.FloodIncident;
import com.oop.disaster.flood_service.service.FloodIncidentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Flood incident REST API.
 *
 * Role rules (enforced here and in SecurityConfig, never only in the UI):
 *  - RECORDER   : creates incidents for their own ward only; sees approved
 *                 incidents plus their own submissions; edits/deletes only
 *                 their own, not-yet-approved records.
 *  - SUPERVISOR : (flood only - checked by JwtAuthenticationFilter) sees all
 *                 flood records and approves / rejects / requests corrections.
 *  - ADMIN      : provincial administrator - read only, may see pending records.
 *  - NATIONAL   : read only, approved records only.
 */
@RestController
@RequestMapping("/api/floods")
public class FloodIncidentController {

    private final FloodIncidentService service;
    private final JwtService jwtService;

    public FloodIncidentController(FloodIncidentService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    /** Identity of the caller, taken from the signed JWT. */
    private record Caller(String username, String role, String ward) {
        boolean is(String r) { return r.equalsIgnoreCase(role); }
    }

    private Caller caller(HttpServletRequest request) {
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

    /** Pending / rejected records are visible only to their recorder, the supervisor and the admin. */
    private boolean isVisible(FloodIncident incident, Caller c) {
        if ("APPROVED".equals(incident.getApprovalStatus())) {
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
        return false; // NATIONAL and anyone else: approved only
    }

    /** Recorders may change only their own records, in their own ward, before approval. */
    private ResponseEntity<Map<String, String>> checkRecorderCanModify(FloodIncident existing, Caller c) {
        if (c.ward() != null && existing.getWard() != null
                && !c.ward().equalsIgnoreCase(existing.getWard())) {
            return forbidden("this incident belongs to a different ward");
        }
        if (c.username() == null || !c.username().equalsIgnoreCase(existing.getReporter())) {
            return forbidden("you can only modify incidents you captured");
        }
        if ("APPROVED".equals(existing.getApprovalStatus())) {
            return forbidden("approved incidents can no longer be modified");
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> createIncident(
            @Valid @RequestBody FloodIncident incident,
            HttpServletRequest request) {

        Caller c = caller(request);

        if (c.ward() != null) {
            if (incident.getWard() == null || incident.getWard().isBlank()) {
                incident.setWard(c.ward());
            } else if (!c.ward().equalsIgnoreCase(incident.getWard())) {
                return forbidden("you can only capture incidents for your own ward (" + c.ward() + ")");
            }
        }
        incident.setReporter(c.username());

        return ResponseEntity.ok(service.createIncident(incident, c.username()));
    }

    @GetMapping
    public List<FloodIncident> getAllIncidents(HttpServletRequest request) {
        Caller c = caller(request);
        return service.getAllIncidents().stream()
                .filter(i -> isVisible(i, c))
                .toList();
    }

    @GetMapping("/approved")
    public List<FloodIncident> getApprovedIncidents() {
        return service.getApprovedIncidents();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FloodIncident> getIncidentById(
            @PathVariable Long id,
            HttpServletRequest request) {

        Optional<FloodIncident> found = service.getIncidentById(id);

        if (found.isEmpty() || !isVisible(found.get(), caller(request))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(found.get());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody FloodIncident incident,
            HttpServletRequest request) {

        Caller c = caller(request);
        Optional<FloodIncident> existing = service.getIncidentById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity<Map<String, String>> denied = checkRecorderCanModify(existing.get(), c);
        if (denied != null) {
            return denied;
        }
        // The ward cannot be moved outside the recorder's own ward.
        if (c.ward() != null && incident.getWard() != null
                && !c.ward().equalsIgnoreCase(incident.getWard())) {
            return forbidden("you can only capture incidents for your own ward (" + c.ward() + ")");
        }

        return ResponseEntity.ok(service.updateIncident(id, incident, c.username()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveIncident(@PathVariable Long id, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(service.approveIncident(id, caller(request).username()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectIncident(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            return ResponseEntity.ok(
                    service.rejectIncident(id, body.get("reason"), caller(request).username()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/corrections")
    public ResponseEntity<?> requestCorrections(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            return ResponseEntity.ok(
                    service.requestCorrections(id, body.get("reason"), caller(request).username()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<FloodAuditLog>> getAuditHistory(
            @PathVariable Long id,
            HttpServletRequest request) {

        Optional<FloodIncident> found = service.getIncidentById(id);
        if (found.isPresent() && !isVisible(found.get(), caller(request))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.getAuditHistory(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIncident(
            @PathVariable Long id,
            HttpServletRequest request) {

        Caller c = caller(request);
        Optional<FloodIncident> existing = service.getIncidentById(id);
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
}
