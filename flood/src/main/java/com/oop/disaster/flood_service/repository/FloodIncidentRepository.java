package com.oop.disaster.flood_service.repository;

import com.oop.disaster.flood_service.model.FloodIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FloodIncidentRepository extends JpaRepository<FloodIncident, Long> {

    List<FloodIncident> findByApprovalStatus(String approvalStatus);
}