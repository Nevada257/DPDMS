package com.oop.disaster.repository;

import com.oop.disaster.entity.FireIncidentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FireIncidentAuditRepository extends JpaRepository<FireIncidentAudit, Long> {
    List<FireIncidentAudit> findByIncidentIdOrderByChangedAtDesc(Long incidentId);
}
