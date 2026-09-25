package com.oop.disaster.repository;

import com.oop.disaster.model.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
}
