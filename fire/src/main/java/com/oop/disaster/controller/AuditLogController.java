package com.oop.disaster.controller;

import com.oop.disaster.model.AuditLog;
import com.oop.disaster.repository.AuditLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogRepository repository;

    public AuditLogController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{incidentId}")
    public List<AuditLog> getIncidentAudit(@PathVariable Long incidentId) {
        return repository.findByIncidentIdOrderByChangedAtDesc(incidentId);
    }
}
