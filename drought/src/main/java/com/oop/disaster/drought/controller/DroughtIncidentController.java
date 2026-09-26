package com.oop.disaster.drought.controller;

import com.oop.disaster.drought.entity.DroughtIncident;
import com.oop.disaster.drought.security.JwtService;
import com.oop.disaster.drought.service.DroughtIncidentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/drought/incidents")
public class DroughtIncidentController {

    private final DroughtIncidentService service;
    private final JwtService jwtService;

    public DroughtIncidentController(DroughtIncidentService service, JwtService jwtService) {
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

    private boolean isVisible(DroughtIncident incident, String role, String username) {
        if (incident.getStatus() != null && incident.getStatus().equals("APPROVED")) {
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

    @PreAuthorize("hasAnyRole('RECORDER','SUPERVISOR','NATIONAL')")
    @GetMapping
    public List<DroughtIncident> getAll(HttpServletRequest request) {
        String token = extractToken(request);
        List<DroughtIncident> all = service.getAllIncidents();

        if (token == null) {
            return all.stream().filter(i -> "APPROVED".equals(i.getStatus())).toList();
        }

        String role = jwtService.extractRole(token);
        String username = jwtService.extractUsername(token);

        return all.stream().filter(i -> isVisible(i, role, username)).toList();
    }

    @PreAuthorize("hasAnyRole('RECORDER','SUPERVISOR','NATIONAL')")
    @GetMapping("/{id}")
    public ResponseEntity<DroughtIncident> getById(@PathVariable Long id, HttpServletRequest request) {

        Optional<DroughtIncident> found = service.getIncidentById(id);

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

    @PreAuthorize("hasRole('RECORDER')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DroughtIncident incident, HttpServletRequest request) {

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

        return ResponseEntity.ok(service.createIncident(incident));
    }

    @PreAuthorize("hasRole('RECORDER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @Valid @RequestBody DroughtIncident incident,
                                     HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role) && ward != null) {
                Optional<DroughtIncident> existing = service.getIncidentById(id);

                if (existing.isPresent()
                        && existing.get().getWard() != null
                        && !ward.equalsIgnoreCase(existing.get().getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: this incident belongs to a different ward")
                    );
                }
            }
        }

        return service.updateIncident(id, incident)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('RECORDER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role) && ward != null) {
                Optional<DroughtIncident> existing = service.getIncidentById(id);

                if (existing.isPresent()
                        && existing.get().getWard() != null
                        && !ward.equalsIgnoreCase(existing.get().getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: this incident belongs to a different ward")
                    );
                }
            }
        }

        boolean deleted = service.deleteIncident(id);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<DroughtIncident> approve(@PathVariable Long id,
                                                   @RequestParam String performedBy) {
        return service.approveIncident(id, performedBy)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<DroughtIncident> reject(@PathVariable Long id,
                                                  @RequestParam String reason,
                                                  @RequestParam String performedBy) {
        return service.rejectIncident(id, reason, performedBy)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/request-correction")
    public ResponseEntity<DroughtIncident> requestCorrection(@PathVariable Long id,
                                                             @RequestParam String notes,
                                                             @RequestParam String performedBy) {
        return service.requestCorrection(id, notes, performedBy)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}