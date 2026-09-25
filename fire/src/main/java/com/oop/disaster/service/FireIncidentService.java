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
    private final AlertService alertService;

    public FireIncidentService(FireIncidentRepository repository,
                               AuditLogRepository auditLogRepository,
                               AlertService alertService) {
        this.repository = repository;
        this.auditLogRepository = auditLogRepository;
        this.alertService = alertService;
    }

    public FireIncident create(FireIncident incident) {
        incident.setStatus(IncidentStatus.PENDING);
        FireIncident saved = repository.save(incident);

        if (saved.isActive()) {
            alertService.dispatchForFire(saved);
        }
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

    public FireIncident update(Long id, FireIncident incoming) {
        FireIncident existing = getOne(id);

        existing.setWard(incoming.getWard());
        existing.setDistrict(incoming.getDistrict());
        existing.setProvince(incoming.getProvince());
        existing.setOccurrenceTime(incoming.getOccurrenceTime());
        existing.setReporter(incoming.getReporter());
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

        if (existing.getStatus() == IncidentStatus.NEEDS_CORRECTION) {
            existing.setStatus(IncidentStatus.PENDING);
        }

        FireIncident saved = repository.save(existing);

        if (saved.isActive()) {
            alertService.dispatchForFire(saved);
        }
        return saved;
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public FireIncident changeStatus(Long id, IncidentStatus newStatus,
                                     String reason, String changedBy) {
        FireIncident incident = getOne(id);
        IncidentStatus oldStatus = incident.getStatus();

        incident.setStatus(newStatus);
        FireIncident saved = repository.save(incident);

        AuditLog log = new AuditLog();
        log.setIncidentId(id);
        log.setChangedBy(changedBy);
        log.setChangedAt(LocalDateTime.now());
        log.setOldStatus(oldStatus.name());
        log.setNewStatus(newStatus.name());
        log.setReason(reason);
        auditLogRepository.save(log);

        return saved;
    }
}
