package com.oop.disaster.mining.controller;

import com.oop.disaster.mining.model.AuditLog;
import com.oop.disaster.mining.model.IncidentStatus;
import com.oop.disaster.mining.model.MiningAccident;
import com.oop.disaster.mining.service.MiningAccidentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mining-accidents")
public class MiningAccidentController {
    private final MiningAccidentService service;
    public MiningAccidentController(MiningAccidentService service) { this.service = service; }

    @PostMapping
    public MiningAccident create(@RequestBody MiningAccident accident, @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) { return service.create(accident, actor); }
    @GetMapping
    public List<MiningAccident> getAll() { return service.findAll(); }
    @GetMapping("/{id}")
    public MiningAccident getById(@PathVariable Long id) { return service.findById(id); }
    @GetMapping("/status/{status}")
    public List<MiningAccident> getByStatus(@PathVariable IncidentStatus status) { return service.findByStatus(status); }
    @PutMapping("/{id}")
    public MiningAccident update(@PathVariable Long id, @RequestBody MiningAccident accident, @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) { return service.update(id, accident, actor); }
    @PostMapping("/{id}/approve")
    public MiningAccident approve(@PathVariable Long id, @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) { return service.approve(id, actor); }
    @PostMapping("/{id}/reject")
    public MiningAccident reject(@PathVariable Long id, @RequestBody Map<String, String> body, @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) { return service.reject(id, body.get("reason"), actor); }
    @PostMapping("/{id}/request-correction")
    public MiningAccident requestCorrection(@PathVariable Long id, @RequestBody Map<String, String> body, @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) { return service.requestCorrection(id, body.get("reason"), actor); }
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }
    @GetMapping("/{id}/audit")
    public List<AuditLog> getAudit(@PathVariable Long id) { return service.getAuditLogs(id); }
}