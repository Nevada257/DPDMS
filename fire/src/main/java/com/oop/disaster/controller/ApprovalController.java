package com.oop.disaster.controller;

import com.oop.disaster.model.FireIncident;
import com.oop.disaster.model.IncidentStatus;
import com.oop.disaster.service.FireIncidentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fire-incidents")
public class ApprovalController {

    private final FireIncidentService service;

    public ApprovalController(FireIncidentService service) {
        this.service = service;
    }

    @PutMapping("/{id}/approve")
    public FireIncident approve(@PathVariable Long id,
                                @RequestParam(defaultValue = "supervisor") String by) {
        return service.changeStatus(id, IncidentStatus.APPROVED, "Approved", by);
    }

    @PutMapping("/{id}/reject")
    public FireIncident reject(@PathVariable Long id,
                               @RequestParam String reason,
                               @RequestParam(defaultValue = "supervisor") String by) {
        return service.changeStatus(id, IncidentStatus.REJECTED, reason, by);
    }

    @PutMapping("/{id}/correction")
    public FireIncident correction(@PathVariable Long id,
                                   @RequestParam String reason,
                                   @RequestParam(defaultValue = "supervisor") String by) {
        return service.changeStatus(id, IncidentStatus.NEEDS_CORRECTION, reason, by);
    }
}
