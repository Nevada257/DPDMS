package com.oop.disaster.controller;

import com.oop.disaster.entity.FireIncident;
import com.oop.disaster.entity.FireIncidentAudit;
import com.oop.disaster.service.FireIncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fire-incidents")
@Tag(name = "FIRE INCIDENT", description = "Basic fire incident operations")
public class FireIncidentController {

    private final FireIncidentService service;

    public FireIncidentController(FireIncidentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "ADD FIRE INCIDENT", description = "Create and save a new fire incident")
    public ResponseEntity<FireIncident> add(@Valid @RequestBody FireIncident incident) {
        return ResponseEntity.ok(service.create(incident));
    }

    @GetMapping
    @Operation(summary = "VIEW ALL FIRE INCIDENTS", description = "Get all fire incidents")
    public List<FireIncident> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "VIEW FIRE INCIDENT", description = "Get one fire incident by ID")
    public ResponseEntity<FireIncident> getOne(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "UPDATE FIRE INCIDENT", description = "Update an existing fire incident")
    public ResponseEntity<FireIncident> update(@PathVariable Long id,
                                               @Valid @RequestBody FireIncident incident) {
        return service.update(id, incident).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "DELETE FIRE INCIDENT", description = "Delete a fire incident by ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id) ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "APPROVE FIRE INCIDENT", description = "Approve a pending fire incident")
    public ResponseEntity<FireIncident> approve(
            @PathVariable Long id,
            @Parameter(description = "Name of the supervisor") @RequestParam(defaultValue = "supervisor") String user) {
        return service.approve(id, user).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "REJECT FIRE INCIDENT", description = "Reject a fire incident with a reason")
    public ResponseEntity<FireIncident> reject(@PathVariable Long id,
                                               @RequestParam(defaultValue = "supervisor") String user,
                                               @RequestParam(required = false) String reason) {
        return service.reject(id, user, reason).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/correction")
    @Operation(summary = "REQUEST CORRECTION", description = "Send an incident back for correction")
    public ResponseEntity<FireIncident> correction(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "supervisor") String user,
                                                   @RequestParam(required = false) String reason) {
        return service.requestCorrection(id, user, reason).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "VIEW AUDIT HISTORY", description = "View changes made to a fire incident")
    public List<FireIncidentAudit> audit(@PathVariable Long id) {
        return service.getAudit(id);
    }
}
