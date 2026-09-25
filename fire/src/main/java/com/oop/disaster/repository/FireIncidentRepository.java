package com.oop.disaster.repository;

import com.oop.disaster.model.FireIncident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FireIncidentRepository extends JpaRepository<FireIncident, Long> {
}
