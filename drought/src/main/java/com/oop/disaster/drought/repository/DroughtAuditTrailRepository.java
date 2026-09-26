package com.oop.disaster.drought.repository;

import com.oop.disaster.drought.entity.DroughtAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DroughtAuditTrailRepository extends JpaRepository<DroughtAuditTrail, Long> {
    List<DroughtAuditTrail> findByIncidentIdOrderByPerformedAtAsc(Long incidentId);
}