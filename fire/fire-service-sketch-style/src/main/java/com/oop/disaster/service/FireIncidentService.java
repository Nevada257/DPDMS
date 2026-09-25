package com.oop.disaster.service;

import com.oop.disaster.entity.FireIncident;
import com.oop.disaster.entity.FireIncidentAudit;
import com.oop.disaster.repository.FireIncidentAuditRepository;
import com.oop.disaster.repository.FireIncidentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FireIncidentService {

    private final FireIncidentRepository repository;
    private final FireIncidentAuditRepository auditRepository;

    public FireIncidentService(FireIncidentRepository repository,
                               FireIncidentAuditRepository auditRepository) {
        this.repository = repository;
        this.auditRepository = auditRepository;
    }

    public FireIncident create(FireIncident incident) {
        if (incident.getStatus() == null || incident.getStatus().isBlank()) {
            incident.setStatus("PENDING");
        }
        FireIncident saved = repository.save(incident);
        audit(saved.getId(), "SUBMITTED", incident.getReporter(), "New fire incident submitted");
        return saved;
    }

    public List<FireIncident> findAll() {
        return repository.findAll();
    }

    public Optional<FireIncident> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<FireIncident> update(Long id, FireIncident incoming) {
        return repository.findById(id).map(existing -> {
            incoming.setId(id);
            FireIncident saved = repository.save(incoming);
            audit(id, "UPDATED", incoming.getReporter(), "Fire incident updated");
            return saved;
        });
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        audit(id, "DELETED", "SYSTEM", "Fire incident deleted");
        return true;
    }

    public Optional<FireIncident> approve(Long id, String user) {
        return changeStatus(id, "APPROVED", user, "Incident approved");
    }

    public Optional<FireIncident> reject(Long id, String user, String reason) {
        return changeStatus(id, "REJECTED", user, reason == null ? "Incident rejected" : reason);
    }

    public Optional<FireIncident> requestCorrection(Long id, String user, String reason) {
        return changeStatus(id, "NEEDS_CORRECTION", user,
                reason == null ? "Correction requested" : reason);
    }

    public List<FireIncidentAudit> getAudit(Long id) {
        return auditRepository.findByIncidentIdOrderByChangedAtDesc(id);
    }

    private Optional<FireIncident> changeStatus(Long id, String status, String user, String reason) {
        return repository.findById(id).map(incident -> {
            incident.setStatus(status);
            FireIncident saved = repository.save(incident);
            audit(id, status, user, reason);
            return saved;
        });
    }

    private void audit(Long id, String action, String user, String reason) {
        auditRepository.save(new FireIncidentAudit(
                id, action, user, LocalDateTime.now(), reason));
    }
}
