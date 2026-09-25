package com.oop.disaster.mining.repository;
import com.oop.disaster.mining.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByAccidentIdOrderByTimestampAsc(Long accidentId);
}