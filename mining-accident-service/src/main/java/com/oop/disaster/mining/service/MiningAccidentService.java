package com.oop.disaster.mining.service;

import com.oop.disaster.mining.model.*;
import com.oop.disaster.mining.repository.AuditLogRepository;
import com.oop.disaster.mining.repository.MiningAccidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Mining accident business logic and approval workflow:
 *   PENDING -> APPROVED
 *   PENDING -> REJECTED            (reason required)
 *   PENDING -> CORRECTION_REQUESTED (reason required) -> edited by recorder -> PENDING
 * Every transition is written to the audit trail with the acting user.
 */
@Service
@Transactional
public class MiningAccidentService {

    private final MiningAccidentRepository accidentRepo;
    private final AuditLogRepository auditRepo;

    public MiningAccidentService(MiningAccidentRepository accidentRepo, AuditLogRepository auditRepo) {
        this.accidentRepo = accidentRepo;
        this.auditRepo = auditRepo;
    }

    public MiningAccident create(MiningAccident accident, String actor) {
        accident.setStatus(IncidentStatus.PENDING);
        accident.setRejectionReason(null);
        MiningAccident saved = accidentRepo.save(accident);
        auditRepo.save(new AuditLog(saved.getId(), actor, "CREATE", null, IncidentStatus.PENDING, "Created"));
        return saved;
    }

    public List<MiningAccident> findAll() {
        return accidentRepo.findAll();
    }

    public MiningAccident findById(Long id) {
        return accidentRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Mining accident not found: " + id));
    }

    public List<MiningAccident> findByStatus(IncidentStatus status) {
        return accidentRepo.findByStatus(status);
    }

    public MiningAccident update(Long id, MiningAccident updated, String actor) {
        MiningAccident existing = findById(id);
        IncidentStatus before = existing.getStatus();

        existing.setWard(updated.getWard());
        existing.setDistrict(updated.getDistrict());
        existing.setProvince(updated.getProvince());
        existing.setOccurrenceDateTime(updated.getOccurrenceDateTime());
        existing.setSeverity(updated.getSeverity());
        existing.setLatitude(updated.getLatitude());
        existing.setLongitude(updated.getLongitude());
        existing.setMineName(updated.getMineName());
        existing.setMineType(updated.getMineType());
        existing.setAccidentType(updated.getAccidentType());
        existing.setTrappedOrInjuredMiners(updated.getTrappedOrInjuredMiners());
        existing.setFatalities(updated.getFatalities());
        existing.setRescueOperationsOngoing(updated.getRescueOperationsOngoing());

        // Editing a record that was sent back for corrections resubmits it for approval.
        if (before == IncidentStatus.CORRECTION_REQUESTED) {
            existing.setStatus(IncidentStatus.PENDING);
            existing.setRejectionReason(null);
        }

        MiningAccident saved = accidentRepo.save(existing);
        auditRepo.save(new AuditLog(id, actor,
                before == IncidentStatus.CORRECTION_REQUESTED ? "RESUBMIT" : "UPDATE",
                before, saved.getStatus(),
                before == IncidentStatus.CORRECTION_REQUESTED
                        ? "Corrections made and resubmitted for approval" : "Edited"));
        return saved;
    }

    public MiningAccident approve(Long id, String actor) {
        return transition(id, IncidentStatus.APPROVED, "APPROVE", "Approved", actor);
    }

    public MiningAccident reject(Long id, String reason, String actor) {
        requireReason(reason);
        return transition(id, IncidentStatus.REJECTED, "REJECT", reason, actor);
    }

    public MiningAccident requestCorrection(Long id, String reason, String actor) {
        requireReason(reason);
        return transition(id, IncidentStatus.CORRECTION_REQUESTED, "CORRECTION_REQUESTED", reason, actor);
    }

    private MiningAccident transition(Long id, IncidentStatus to, String action, String details, String actor) {
        MiningAccident accident = findById(id);
        IncidentStatus prev = accident.getStatus();

        if (prev != IncidentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING accidents can be moved to " + to + " (current: " + prev + ")");
        }

        accident.setStatus(to);
        accident.setRejectionReason(to == IncidentStatus.APPROVED ? null : details);
        MiningAccident saved = accidentRepo.save(accident);
        auditRepo.save(new AuditLog(id, actor, action, prev, to, details));
        return saved;
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }
    }

    public void delete(Long id, String actor) {
        MiningAccident existing = findById(id);
        accidentRepo.deleteById(id);
        auditRepo.save(new AuditLog(id, actor, "DELETE", existing.getStatus(), null, "Deleted"));
    }

    public List<AuditLog> getAuditLogs(Long accidentId) {
        return auditRepo.findByAccidentIdOrderByTimestampAsc(accidentId);
    }
}
