package com.oop.disaster.controller;

import com.oop.disaster.model.AlertLog;
import com.oop.disaster.repository.AlertLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertLogController {

    private final AlertLogRepository repository;

    public AlertLogController(AlertLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/logs")
    public List<AlertLog> getLogs() {
        return repository.findAll();
    }
}
