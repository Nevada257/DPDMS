package com.oop.disaster.flood_service.service;

import com.oop.disaster.flood_service.model.FloodAuditLog;
import com.oop.disaster.flood_service.model.FloodIncident;
import com.oop.disaster.flood_service.repository.FloodAuditLogRepository;
import com.oop.disaster.flood_service.repository.FloodIncidentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FloodIncidentService {

    private final FloodIncidentRepository repository;
    private final FloodAuditLogRepository auditRepository;

    public FloodIncidentService(
            FloodIncidentRepository repository,
            FloodAuditLogRepository auditRepository) {

        this.repository = repository;
        this.auditRepository = auditRepository;
    }

    // CREATE
    public FloodIncident createIncident(FloodIncident incident) {

        incident.setApprovalStatus("PENDING");
        incident.setRejectionReason(null);

        FloodIncident saved = repository.save(incident);

        saveAudit(
                saved.getId(),
                "SUBMITTED",
                "RECORDER",
                "New flood incident submitted"
        );

        return saved;
    }

    // GET ALL
    public List<FloodIncident> getAllIncidents() {
        return repository.findAll();
    }

    // GET APPROVED ONLY
    public List<FloodIncident> getApprovedIncidents() {
        return repository.findByApprovalStatus("APPROVED");
    }

    // GET BY ID
    public Optional<FloodIncident> getIncidentById(Long id) {
        return repository.findById(id);
    }

    // UPDATE
    public FloodIncident updateIncident(Long id, FloodIncident incident) {

        FloodIncident existing = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Flood incident not found"));

        existing.setWard(incident.getWard());
        existing.setDistrict(incident.getDistrict());
        existing.setProvince(incident.getProvince());
        existing.setOccurrenceDateTime(incident.getOccurrenceDateTime());
        existing.setReporter(incident.getReporter());
        existing.setSeverity(incident.getSeverity());
        existing.setStatus(incident.getStatus());
        existing.setLatitude(incident.getLatitude());
        existing.setLongitude(incident.getLongitude());
        existing.setPeakWaterLevel(incident.getPeakWaterLevel());
        existing.setRiverBasin(incident.getRiverBasin());
        existing.setHouseholdsDisplaced(incident.getHouseholdsDisplaced());
        existing.setAreaFlooded(incident.getAreaFlooded());
        existing.setDurationOfInundation(incident.getDurationOfInundation());

        // If corrections were requested, editing the incident
        // automatically sends it back to PENDING.
        if ("CORRECTIONS_REQUESTED".equals(existing.getApprovalStatus())) {

            existing.setApprovalStatus("PENDING");
            existing.setRejectionReason(null);

            FloodIncident saved = repository.save(existing);

            saveAudit(
                    saved.getId(),
                    "RESUBMITTED",
                    "RECORDER",
                    "Corrections made and incident resubmitted for approval"
            );

            return saved;
        }

        return repository.save(existing);
    }

    // APPROVE
    public FloodIncident approveIncident(Long id) {

        FloodIncident incident = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Flood incident not found"));

        if (!"PENDING".equals(incident.getApprovalStatus())) {
            throw new RuntimeException(
                    "Only PENDING incidents can be approved");
        }

        incident.setApprovalStatus("APPROVED");
        incident.setRejectionReason(null);

        FloodIncident saved = repository.save(incident);

        saveAudit(
                saved.getId(),
                "APPROVED",
                "PROVINCIAL_SUPERVISOR",
                "Flood incident approved"
        );

        return saved;
    }

    // REJECT
    public FloodIncident rejectIncident(Long id, String reason) {

        FloodIncident incident = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Flood incident not found"));

        if (!"PENDING".equals(incident.getApprovalStatus())) {
            throw new RuntimeException(
                    "Only PENDING incidents can be rejected");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException(
                    "Rejection reason is required");
        }

        incident.setApprovalStatus("REJECTED");
        incident.setRejectionReason(reason);

        FloodIncident saved = repository.save(incident);

        saveAudit(
                saved.getId(),
                "REJECTED",
                "PROVINCIAL_SUPERVISOR",
                reason
        );

        return saved;
    }

    // REQUEST CORRECTIONS
    public FloodIncident requestCorrections(Long id, String reason) {

        FloodIncident incident = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Flood incident not found"));

        if (!"PENDING".equals(incident.getApprovalStatus())) {
            throw new RuntimeException(
                    "Only PENDING incidents can have corrections requested");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException(
                    "Correction reason is required");
        }

        incident.setApprovalStatus("CORRECTIONS_REQUESTED");
        incident.setRejectionReason(reason);

        FloodIncident saved = repository.save(incident);

        saveAudit(
                saved.getId(),
                "CORRECTIONS_REQUESTED",
                "PROVINCIAL_SUPERVISOR",
                reason
        );

        return saved;
    }

    // AUDIT HISTORY
    public List<FloodAuditLog> getAuditHistory(Long incidentId) {
        return auditRepository.findByIncidentId(incidentId);
    }

    // DELETE
    public void deleteIncident(Long id) {
        repository.deleteById(id);
    }

    // SAVE AUDIT RECORD
    private void saveAudit(
            Long incidentId,
            String action,
            String performedBy,
            String details) {

        FloodAuditLog audit = new FloodAuditLog();

        audit.setIncidentId(incidentId);
        audit.setAction(action);
        audit.setPerformedBy(performedBy);
        audit.setDetails(details);
        audit.setTimestamp(LocalDateTime.now());

        auditRepository.save(audit);
    }
}