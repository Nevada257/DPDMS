package com.oop.disaster.mining.repository;

import com.oop.disaster.mining.model.IncidentStatus;
import com.oop.disaster.mining.model.MiningAccident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MiningAccidentRepository extends JpaRepository<MiningAccident, Long> {
    List<MiningAccident> findByStatus(IncidentStatus status);
}