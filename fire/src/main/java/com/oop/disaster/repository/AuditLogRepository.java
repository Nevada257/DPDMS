package com.oop.disaster.repository;

import com.oop.disaster.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByIncidentIdOrderByChangedAtDesc(Long incidentId);
}
