package com.oop.disaster.drought.service;

import com.oop.disaster.drought.entity.DroughtIncident;
import com.oop.disaster.drought.entity.DroughtAuditTrail;
import com.oop.disaster.drought.repository.DroughtIncidentRepository;
import com.oop.disaster.drought.repository.DroughtAuditTrailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DroughtIncidentService {

    private final DroughtIncidentRepository repository;
    private final DroughtAuditTrailRepository auditRepository;

    public DroughtIncidentService(DroughtIncidentRepository repository,
                                  DroughtAuditTrailRepository auditRepository) {
        this.repository = repository;
        this.auditRepository = auditRepository;
    }

    public List<DroughtIncident> getAllIncidents() {
        return repository.findAll();
    }

    public Optional<DroughtIncident> getIncidentById(Long id) {
        return repository.findById(id);
    }

    public DroughtIncident createIncident(DroughtIncident incident) {
        incident.setStatus("PENDING");
        DroughtIncident saved = repository.save(incident);
        logAudit(saved.getId(), "CREATED", null, "PENDING", saved.getReporter(), null);
        return saved;
    }

    /**
     * Updates an incident's data. The approval status can never be set through
     * an edit; editing a record sent back for corrections resubmits it as PENDING.
     */
    public Optional<DroughtIncident> updateIncident(Long id, DroughtIncident incident, String performedBy) {
        return repository.findById(id).map(existing -> {
            String oldStatus = existing.getStatus();

            existing.setRainfallDeficitMm(incident.getRainfallDeficitMm());
            existing.setConsecutiveDryDays(incident.getConsecutiveDryDays());
            existing.setCropFailurePercentage(incident.getCropFailurePercentage());
            existing.setPeopleFacingWaterShortages(incident.getPeopleFacingWaterShortages());
            existing.setLivestockMortalityCount(incident.getLivestockMortalityCount());
            existing.setWard(incident.getWard());
            existing.setDistrict(incident.getDistrict());
            existing.setProvince(incident.getProvince());
            existing.setDateTimeOfOccurrence(incident.getDateTimeOfOccurrence());
            existing.setSeverity(incident.getSeverity());
            existing.setLatitude(incident.getLatitude());
            existing.setLongitude(incident.getLongitude());

            boolean resubmitted = "CORRECTION_REQUESTED".equals(oldStatus);
            if (resubmitted) {
                existing.setStatus("PENDING");
                existing.setRejectionReason(null);
            }

            DroughtIncident saved = repository.save(existing);
            logAudit(saved.getId(), resubmitted ? "RESUBMITTED" : "UPDATED",
                    oldStatus, saved.getStatus(), performedBy, null);
            return saved;
        });
    }

    public boolean deleteIncident(Long id, String performedBy) {
        Optional<DroughtIncident> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return false;
        }
        repository.deleteById(id);
        logAudit(id, "DELETED", existing.get().getStatus(), null, performedBy, null);
        return true;
    }

    public List<DroughtAuditTrail> getAuditTrail(Long incidentId) {
        return auditRepository.findByIncidentIdOrderByPerformedAtAsc(incidentId);
    }

    // ---------- Workflow ----------

    public Optional<DroughtIncident> approveIncident(Long id, String performedBy) {
        return repository.findById(id).map(incident -> {
            if (!"PENDING".equals(incident.getStatus())) {
                throw new IllegalStateException("Only PENDING incidents can be approved");
            }
            String oldStatus = incident.getStatus();
            incident.setStatus("APPROVED");
            incident.setRejectionReason(null);
            DroughtIncident saved = repository.save(incident);
            logAudit(saved.getId(), "APPROVED", oldStatus, "APPROVED", performedBy, null);
            return saved;
        });
    }

    public Optional<DroughtIncident> rejectIncident(Long id, String reason, String performedBy) {
        requireReason(reason);
        return repository.findById(id).map(incident -> {
            if (!"PENDING".equals(incident.getStatus())) {
                throw new IllegalStateException("Only PENDING incidents can be rejected");
            }
            String oldStatus = incident.getStatus();
            incident.setStatus("REJECTED");
            incident.setRejectionReason(reason);
            DroughtIncident saved = repository.save(incident);
            logAudit(saved.getId(), "REJECTED", oldStatus, "REJECTED", performedBy, reason);
            return saved;
        });
    }

    public Optional<DroughtIncident> requestCorrection(Long id, String notes, String performedBy) {
        requireReason(notes);
        return repository.findById(id).map(incident -> {
            if (!"PENDING".equals(incident.getStatus())) {
                throw new IllegalStateException("Only PENDING incidents can have corrections requested");
            }
            String oldStatus = incident.getStatus();
            incident.setStatus("CORRECTION_REQUESTED");
            incident.setRejectionReason(notes);
            DroughtIncident saved = repository.save(incident);
            logAudit(saved.getId(), "CORRECTION_REQUESTED", oldStatus,
                    "CORRECTION_REQUESTED", performedBy, notes);
            return saved;
        });
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }
    }

    // ---------- Audit helper ----------

    private void logAudit(Long incidentId, String action, String oldStatus,
                          String newStatus, String performedBy, String reason) {
        DroughtAuditTrail trail = new DroughtAuditTrail();
        trail.setIncidentId(incidentId);
        trail.setAction(action);
        trail.setOldStatus(oldStatus);
        trail.setNewStatus(newStatus);
        trail.setPerformedBy(performedBy);
        trail.setReason(reason);
        trail.setPerformedAt(LocalDateTime.now());
        auditRepository.save(trail);
    }
}