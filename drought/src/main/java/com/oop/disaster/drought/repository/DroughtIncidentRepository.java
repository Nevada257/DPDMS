package com.oop.disaster.drought.repository;

import com.oop.disaster.drought.entity.DroughtIncident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DroughtIncidentRepository extends JpaRepository<DroughtIncident, Long> {
}
