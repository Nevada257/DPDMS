package com.oop.disaster.flood_service.controller;

import com.oop.disaster.flood_service.model.FloodAuditLog;
import com.oop.disaster.flood_service.model.FloodIncident;
import com.oop.disaster.flood_service.service.FloodIncidentService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/floods")
public class FloodIncidentController {

    private final FloodIncidentService service;

    public FloodIncidentController(FloodIncidentService service) {
        this.service = service;
    }

    // CREATE FLOOD INCIDENT
    @PostMapping
    public ResponseEntity<FloodIncident> createIncident(
            @Valid @RequestBody FloodIncident incident) {

        return ResponseEntity.ok(
                service.createIncident(incident)
        );
    }

    // GET ALL FLOOD INCIDENTS
    @GetMapping
    public List<FloodIncident> getAllIncidents() {
        return service.getAllIncidents();
    }

    // GET APPROVED FLOOD INCIDENTS ONLY
    @GetMapping("/approved")
    public List<FloodIncident> getApprovedIncidents() {
        return service.getApprovedIncidents();
    }

    // GET FLOOD INCIDENT BY ID
    @GetMapping("/{id}")
    public ResponseEntity<FloodIncident> getIncidentById(
            @PathVariable Long id) {

        return service.getIncidentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE FLOOD INCIDENT
    @PutMapping("/{id}")
    public ResponseEntity<FloodIncident> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody FloodIncident incident) {

        try {
            return ResponseEntity.ok(
                    service.updateIncident(id, incident)
            );

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // APPROVE FLOOD INCIDENT
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

    // REJECT FLOOD INCIDENT
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

    // REQUEST CORRECTIONS
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

    // GET AUDIT HISTORY
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<FloodAuditLog>> getAuditHistory(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.getAuditHistory(id)
        );
    }

    // DELETE FLOOD INCIDENT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable Long id) {

        service.deleteIncident(id);

        return ResponseEntity.noContent().build();
    }
}