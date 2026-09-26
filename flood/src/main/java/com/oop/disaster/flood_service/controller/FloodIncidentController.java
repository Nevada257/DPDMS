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

@RestController
@RequestMapping("/api/floods")
public class FloodIncidentController {

    private final FloodIncidentService service;
    private final JwtService jwtService;

    public FloodIncidentController(FloodIncidentService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private boolean isVisible(FloodIncident incident, String role, String username) {
        if (incident.getApprovalStatus() != null && incident.getApprovalStatus().equals("APPROVED")) {
            return true;
        }
        if ("SUPERVISOR".equalsIgnoreCase(role)) {
            return true;
        }
        if ("NATIONAL".equalsIgnoreCase(role)) {
            return false;
        }
        return username != null && username.equalsIgnoreCase(incident.getReporter());
    }

    @PostMapping
    public ResponseEntity<?> createIncident(
            @Valid @RequestBody FloodIncident incident,
            HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);
            String username = jwtService.extractUsername(token);

            if ("RECORDER".equalsIgnoreCase(role)) {

                if (ward != null
                        && incident.getWard() != null
                        && !ward.equalsIgnoreCase(incident.getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: you can only capture incidents for your own ward (" + ward + ")")
                    );
                }

                incident.setReporter(username);
            }
        }

        return ResponseEntity.ok(
                service.createIncident(incident)
        );
    }

    @GetMapping
    public List<FloodIncident> getAllIncidents(HttpServletRequest request) {

        String token = extractToken(request);
        List<FloodIncident> all = service.getAllIncidents();

        if (token == null) {
            return all.stream().filter(i -> "APPROVED".equals(i.getApprovalStatus())).toList();
        }

        String role = jwtService.extractRole(token);
        String username = jwtService.extractUsername(token);

        return all.stream()
                .filter(i -> isVisible(i, role, username))
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

        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String token = extractToken(request);
        String role = token != null ? jwtService.extractRole(token) : null;
        String username = token != null ? jwtService.extractUsername(token) : null;

        if (!isVisible(found.get(), role, username)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(found.get());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody FloodIncident incident,
            HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role) && ward != null) {
                Optional<FloodIncident> existing = service.getIncidentById(id);

                if (existing.isPresent()
                        && existing.get().getWard() != null
                        && !ward.equalsIgnoreCase(existing.get().getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: this incident belongs to a different ward")
                    );
                }
            }
        }

        try {
            return ResponseEntity.ok(
                    service.updateIncident(id, incident)
            );

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveIncident(
            @PathVariable Long id) {

        try {
            return ResponseEntity.ok(
                    service.approveIncident(id)
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectIncident(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        try {
            String reason = request.get("reason");

            return ResponseEntity.ok(
                    service.rejectIncident(id, reason)
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    @PostMapping("/{id}/corrections")
    public ResponseEntity<?> requestCorrections(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        try {
            String reason = request.get("reason");

            return ResponseEntity.ok(
                    service.requestCorrections(id, reason)
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<FloodAuditLog>> getAuditHistory(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.getAuditHistory(id)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIncident(
            @PathVariable Long id,
            HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role) && ward != null) {
                Optional<FloodIncident> existing = service.getIncidentById(id);

                if (existing.isPresent()
                        && existing.get().getWard() != null
                        && !ward.equalsIgnoreCase(existing.get().getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: this incident belongs to a different ward")
                    );
                }
            }
        }

        service.deleteIncident(id);

        return ResponseEntity.noContent().build();
    }
}