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

    // ---------- CRUD ----------

    @PreAuthorize("hasAnyRole('RECORDER','SUPERVISOR','NATIONAL')")
    @GetMapping
    public List<DroughtIncident> getAll() {
        return service.getAllIncidents();
    }

    @PreAuthorize("hasAnyRole('RECORDER','SUPERVISOR','NATIONAL')")
    @GetMapping("/{id}")
    public ResponseEntity<DroughtIncident> getById(@PathVariable Long id) {
        return service.getIncidentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('RECORDER')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DroughtIncident incident, HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role)
                    && ward != null
                    && incident.getWard() != null
                    && !ward.equalsIgnoreCase(incident.getWard())) {

                return ResponseEntity.status(403).body(
                        Map.of("error", "Forbidden: you can only capture incidents for your own ward (" + ward + ")")
                );
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

    // ---------- Workflow ----------

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