package com.oop.disaster.mining.controller;

import com.oop.disaster.mining.JwtService;
import com.oop.disaster.mining.model.AuditLog;
import com.oop.disaster.mining.model.IncidentStatus;
import com.oop.disaster.mining.model.MiningAccident;
import com.oop.disaster.mining.service.MiningAccidentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mining-accidents")
public class MiningAccidentController {
    private final MiningAccidentService service;
    private final JwtService jwtService;

    public MiningAccidentController(MiningAccidentService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MiningAccident accident,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor,
                                     HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role)
                    && ward != null
                    && accident.getWard() != null
                    && !ward.equalsIgnoreCase(accident.getWard())) {

                return ResponseEntity.status(403).body(
                        Map.of("error", "Forbidden: you can only capture incidents for your own ward (" + ward + ")")
                );
            }
        }

        return ResponseEntity.ok(service.create(accident, actor));
    }

    @GetMapping
    public List<MiningAccident> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public MiningAccident getById(@PathVariable Long id) { return service.findById(id); }

    @GetMapping("/status/{status}")
    public List<MiningAccident> getByStatus(@PathVariable IncidentStatus status) { return service.findByStatus(status); }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MiningAccident accident,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor,
                                     HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role) && ward != null) {
                MiningAccident existing = service.findById(id);

                if (existing.getWard() != null
                        && !ward.equalsIgnoreCase(existing.getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: this incident belongs to a different ward")
                    );
                }
            }
        }

        return ResponseEntity.ok(service.update(id, accident, actor));
    }

    @PostMapping("/{id}/approve")
    public MiningAccident approve(@PathVariable Long id, @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) { return service.approve(id, actor); }
    @PostMapping("/{id}/reject")
    public MiningAccident reject(@PathVariable Long id, @RequestBody Map<String, String> body, @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) { return service.reject(id, body.get("reason"), actor); }
    @PostMapping("/{id}/request-correction")
    public MiningAccident requestCorrection(@PathVariable Long id, @RequestBody Map<String, String> body, @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) { return service.requestCorrection(id, body.get("reason"), actor); }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role) && ward != null) {
                MiningAccident existing = service.findById(id);

                if (existing.getWard() != null
                        && !ward.equalsIgnoreCase(existing.getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: this incident belongs to a different ward")
                    );
                }
            }
        }

        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/audit")
    public List<AuditLog> getAudit(@PathVariable Long id) { return service.getAuditLogs(id); }
}