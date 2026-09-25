package com.oop.disaster.repository;

import com.oop.disaster.entity.FireIncident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FireIncidentRepository extends JpaRepository<FireIncident, Long> {
}
