package com.oop.disaster.alert.repository;

import com.oop.disaster.alert.model.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    List<AlertLog> findAllByOrderBySentAtDesc();

    List<AlertLog> findByHazardIgnoreCaseOrderBySentAtDesc(String hazard);
}
