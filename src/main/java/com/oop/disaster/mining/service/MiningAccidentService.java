package com.oop.disaster.mining.service;
import com.oop.disaster.mining.model.*;
import com.oop.disaster.mining.repository.AuditLogRepository;
import com.oop.disaster.mining.repository.MiningAccidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional
public class MiningAccidentService {
    private final MiningAccidentRepository accidentRepo;
    private final AuditLogRepository auditRepo;
    public MiningAccidentService(MiningAccidentRepository accidentRepo, AuditLogRepository auditRepo) {
        this.accidentRepo = accidentRepo; this.auditRepo = auditRepo;
    }
    public MiningAccident create(MiningAccident accident, String actor) {
        accident.setStatus(IncidentStatus.PENDING);
        MiningAccident saved = accidentRepo.save(accident);
        auditRepo.save(new AuditLog(saved.getId(), actor, "CREATE", null, IncidentStatus.PENDING, "Created"));
        return saved;
    }
    public List<MiningAccident> findAll() { return accidentRepo.findAll(); }
    public MiningAccident findById(Long id) { return accidentRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found: " + id)); }
    public List<MiningAccident> findByStatus(IncidentStatus status) { return accidentRepo.findByStatus(status); }
    public MiningAccident update(Long id, MiningAccident updated, String actor) {
        MiningAccident existing = findById(id);
        existing.setWard(updated.getWard()); existing.setDistrict(updated.getDistrict()); existing.setProvince(updated.getProvince());
        existing.setOccurrenceDateTime(updated.getOccurrenceDateTime()); existing.setReporter(updated.getReporter());
        existing.setSeverity(updated.getSeverity()); existing.setLatitude(updated.getLatitude()); existing.setLongitude(updated.getLongitude());
        existing.setMineName(updated.getMineName()); existing.setMineType(updated.getMineType()); existing.setAccidentType(updated.getAccidentType());
        existing.setTrappedOrInjuredMiners(updated.getTrappedOrInjuredMiners()); existing.setFatalities(updated.getFatalities());
        existing.setRescueOperationsOngoing(updated.getRescueOperationsOngoing());
        return accidentRepo.save(existing);
    }
    public MiningAccident approve(Long id, String actor) {
        MiningAccident e = findById(id); IncidentStatus prev = e.getStatus(); e.setStatus(IncidentStatus.APPROVED); e.setRejectionReason(null);
        MiningAccident s = accidentRepo.save(e); auditRepo.save(new AuditLog(id, actor, "APPROVE", prev, IncidentStatus.APPROVED, "Approved")); return s;
    }
    public MiningAccident reject(Long id, String reason, String actor) {
        MiningAccident e = findById(id); IncidentStatus prev = e.getStatus(); e.setStatus(IncidentStatus.REJECTED); e.setRejectionReason(reason);
        MiningAccident s = accidentRepo.save(e); auditRepo.save(new AuditLog(id, actor, "REJECT", prev, IncidentStatus.REJECTED, reason)); return s;
    }
    public MiningAccident requestCorrection(Long id, String reason, String actor) {
        MiningAccident e = findById(id); IncidentStatus prev = e.getStatus(); e.setStatus(IncidentStatus.CORRECTION_REQUESTED); e.setRejectionReason(reason);
        MiningAccident s = accidentRepo.save(e); auditRepo.save(new AuditLog(id, actor, "CORRECTION_REQUESTED", prev, IncidentStatus.CORRECTION_REQUESTED, reason)); return s;
    }
    public void delete(Long id) { accidentRepo.deleteById(id); }
    public List<AuditLog> getAuditLogs(Long accidentId) { return auditRepo.findByAccidentIdOrderByTimestampAsc(accidentId); }
}