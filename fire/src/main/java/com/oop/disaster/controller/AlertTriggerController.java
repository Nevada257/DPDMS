package com.oop.disaster.controller;

import com.oop.disaster.model.FireIncident;
import com.oop.disaster.service.AlertService;
import com.oop.disaster.service.FireIncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertTriggerController {

    private final FireIncidentService fireIncidentService;
    private final AlertService alertService;

    public AlertTriggerController(FireIncidentService fireIncidentService,
                                  AlertService alertService) {
        this.fireIncidentService = fireIncidentService;
        this.alertService = alertService;
    }

    @PostMapping("/fire/{incidentId}")
    public ResponseEntity<String> trigger(@PathVariable Long incidentId) {
        FireIncident incident = fireIncidentService.getOne(incidentId);
        alertService.dispatchForFire(incident);
        return ResponseEntity.accepted().body("Alert dispatch started");
    }
}
