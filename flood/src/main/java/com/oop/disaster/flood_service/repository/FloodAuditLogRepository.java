package com.oop.disaster.flood_service.repository;

import com.oop.disaster.flood_service.model.FloodAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FloodAuditLogRepository extends JpaRepository<FloodAuditLog, Long> {

    List<FloodAuditLog> findByIncidentId(Long incidentId);
}