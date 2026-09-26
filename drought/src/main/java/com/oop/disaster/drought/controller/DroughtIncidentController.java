package com.oop.disaster.drought.controller;

import com.oop.disaster.drought.entity.DroughtIncident;
import com.oop.disaster.drought.service.DroughtIncidentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drought/incidents")
public class DroughtIncidentController {

    private final DroughtIncidentService service;

    public DroughtIncidentController(DroughtIncidentService service) {
        this.service = service;
    }

    // ---------- CRUD ----------

    @PreAuthorize("hasAnyRole('DROUGHT_RECORDER','DROUGHT_SUPERVISOR','PROVINCIAL_ADMIN','NATIONAL_USER')")
    @GetMapping
    public List<DroughtIncident> getAll() {
        return service.getAllIncidents();
    }

    @PreAuthorize("hasAnyRole('DROUGHT_RECORDER','DROUGHT_SUPERVISOR','PROVINCIAL_ADMIN','NATIONAL_USER')")
    @GetMapping("/{id}")
    public ResponseEntity<DroughtIncident> getById(@PathVariable Long id) {
        return service.getIncidentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('DROUGHT_RECORDER','PROVINCIAL_ADMIN')")
    @PostMapping
    public DroughtIncident create(@Valid @RequestBody DroughtIncident incident) {
        return service.createIncident(incident);
    }

    @PreAuthorize("hasAnyRole('DROUGHT_RECORDER','PROVINCIAL_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DroughtIncident> update(@PathVariable Long id,
                                                  @Valid @RequestBody DroughtIncident incident) {
        return service.updateIncident(id, incident)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('PROVINCIAL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = service.deleteIncident(id);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // ---------- Workflow ----------

    @PreAuthorize("hasRole('DROUGHT_SUPERVISOR')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<DroughtIncident> approve(@PathVariable Long id,
                                                   @RequestParam String performedBy) {
        return service.approveIncident(id, performedBy)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('DROUGHT_SUPERVISOR')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<DroughtIncident> reject(@PathVariable Long id,
                                                  @RequestParam String reason,
                                                  @RequestParam String performedBy) {
        return service.rejectIncident(id, reason, performedBy)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('DROUGHT_SUPERVISOR')")
    @PatchMapping("/{id}/request-correction")
    public ResponseEntity<DroughtIncident> requestCorrection(@PathVariable Long id,
                                                             @RequestParam String notes,
                                                             @RequestParam String performedBy) {
        return service.requestCorrection(id, notes, performedBy)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}