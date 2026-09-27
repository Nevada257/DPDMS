package com.oop.disaster.service;

import com.oop.disaster.model.*;
import com.oop.disaster.model.AuditLog;
import com.oop.disaster.model.FireIncident;
import com.oop.disaster.model.IncidentStatus;
import com.oop.disaster.repository.AuditLogRepository;
import com.oop.disaster.repository.FireIncidentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FireIncidentService {

    private final FireIncidentRepository repository;
    private final AuditLogRepository auditLogRepository;
    public FireIncidentService(FireIncidentRepository repository,
                               AuditLogRepository auditLogRepository) {
        this.repository = repository;
        this.auditLogRepository = auditLogRepository;
    }

    public FireIncident create(FireIncident incident, String actor) {
        incident.setStatus(IncidentStatus.PENDING);
        FireIncident saved = repository.save(incident);
        audit(saved.getId(), actor, null, IncidentStatus.PENDING, "Fire incident captured");
        return saved;
    }

    public List<FireIncident> getAllVisible() {
        return repository.findAll().stream()
                .filter(i -> i.getStatus() != IncidentStatus.PENDING)
                .toList();
    }

    public List<FireIncident> getAll() {
        return repository.findAll();
    }

    public FireIncident getOne(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fire incident not found"));
    }

    public FireIncident update(Long id, FireIncident incoming, String actor) {
        FireIncident existing = getOne(id);

        existing.setWard(incoming.getWard());
        existing.setDistrict(incoming.getDistrict());
        existing.setProvince(incoming.getProvince());
        existing.setOccurrenceTime(incoming.getOccurrenceTime());
        existing.setReporterEmail(incoming.getReporterEmail());
        existing.setReporterPhone(incoming.getReporterPhone());
        existing.setSeverity(incoming.getSeverity());
        existing.setLatitude(incoming.getLatitude());
        existing.setLongitude(incoming.getLongitude());
        existing.setAreaBurned(incoming.getAreaBurned());
        existing.setSuspectedCause(incoming.getSuspectedCause());
        existing.setInjuriesOrFatalities(incoming.getInjuriesOrFatalities());
        existing.setStructuresDestroyed(incoming.getStructuresDestroyed());
        existing.setActive(incoming.isActive());

        IncidentStatus before = existing.getStatus();
        if (before == IncidentStatus.NEEDS_CORRECTION) {
            existing.setStatus(IncidentStatus.PENDING);
        }

        FireIncident saved = repository.save(existing);
        audit(id, actor, before, saved.getStatus(),
                before == IncidentStatus.NEEDS_CORRECTION
                        ? "Corrections made and incident resubmitted for approval"
                        : "Fire incident edited");
        return saved;
    }

    public void delete(Long id, String actor) {
        FireIncident existing = getOne(id);
        repository.deleteById(id);
        audit(id, actor, existing.getStatus(), null, "Fire incident deleted");
    }

    public FireIncident changeStatus(Long id, IncidentStatus newStatus,
                                     String reason, String changedBy) {
        FireIncident incident = getOne(id);
        IncidentStatus oldStatus = incident.getStatus();

        // Only records awaiting a decision can be approved, rejected or sent back.
        if (oldStatus != IncidentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING incidents can be moved to " + newStatus + " (current: " + oldStatus + ")");
        }
        if (newStatus != IncidentStatus.APPROVED && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A reason is required");
        }

        incident.setStatus(newStatus);
        FireIncident saved = repository.save(incident);
        audit(id, changedBy, oldStatus, newStatus, reason);
        return saved;
    }

    private void audit(Long incidentId, String actor, IncidentStatus from, IncidentStatus to, String reason) {
        AuditLog log = new AuditLog();
        log.setIncidentId(incidentId);
        log.setChangedBy(actor);
        log.setChangedAt(LocalDateTime.now());
        log.setOldStatus(from == null ? null : from.name());
        log.setNewStatus(to == null ? "DELETED" : to.name());
        log.setReason(reason);
        auditLogRepository.save(log);
    }
}
