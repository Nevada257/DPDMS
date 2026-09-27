package com.oop.disaster.mining.controller;

import com.oop.disaster.mining.JwtService;
import com.oop.disaster.mining.model.AuditLog;
import com.oop.disaster.mining.model.IncidentStatus;
import com.oop.disaster.mining.model.MiningAccident;
import com.oop.disaster.mining.service.MiningAccidentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.oop.disaster.mining.service.AlertClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mining accident REST API.
 *
 * Role rules (enforced here and in SecurityConfig, never only in the UI):
 *  - RECORDER   : creates accidents for their own ward only; sees approved
 *                 records plus their own submissions; edits/deletes only
 *                 their own, not-yet-approved records.
 *  - SUPERVISOR : (mining only - checked by JwtAuthenticationFilter) sees all
 *                 mining records and approves / rejects / requests corrections.
 *  - ADMIN      : provincial administrator - read only, may see pending records.
 *  - NATIONAL   : read only, approved records only.
 * The acting user recorded in the audit trail always comes from the signed JWT.
 */
@RestController
@RequestMapping("/api/mining-accidents")
public class MiningAccidentController {

    private final MiningAccidentService service;
    private final JwtService jwtService;
    private final AlertClient alertClient;

    public MiningAccidentController(MiningAccidentService service, JwtService jwtService,
                                    AlertClient alertClient) {
        this.service = service;
        this.jwtService = jwtService;
        this.alertClient = alertClient;
    }

    record Caller(String username, String role, String ward) {
        boolean is(String r) { return r.equalsIgnoreCase(role); }
    }

    Caller caller(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return new Caller(null, null, null);
        }
        String token = header.substring(7);
        return new Caller(
                jwtService.extractUsername(token),
                jwtService.extractRole(token),
                jwtService.extractWard(token));
    }

    private static ResponseEntity<Map<String, String>> forbidden(String message) {
        return ResponseEntity.status(403).body(Map.of("error", "Forbidden: " + message));
    }

    private boolean isVisible(MiningAccident accident, Caller c) {
        if (accident.getStatus() == IncidentStatus.APPROVED) {
            return true;
        }
        if (c.role() == null) {
            return false;
        }
        if (c.is("SUPERVISOR") || c.is("ADMIN")) {
            return true;
        }
        if (c.is("RECORDER")) {
            return c.username() != null && c.username().equalsIgnoreCase(accident.getReporter());
        }
        return false; // NATIONAL: approved only
    }

    private ResponseEntity<Map<String, String>> checkRecorderCanModify(MiningAccident existing, Caller c) {
        if (c.ward() != null && existing.getWard() != null
                && !c.ward().equalsIgnoreCase(existing.getWard())) {
            return forbidden("this incident belongs to a different ward");
        }
        if (c.username() == null || !c.username().equalsIgnoreCase(existing.getReporter())) {
            return forbidden("you can only modify incidents you captured");
        }
        if (existing.getStatus() == IncidentStatus.APPROVED) {
            return forbidden("approved incidents can no longer be modified");
        }
        return null;
    }

    /** Sends the accident to the alert-service, which decides whether it meets the alert criteria. */
    private MiningAccident raiseAlert(MiningAccident a) {
        Map<String, Object> indicators = new LinkedHashMap<>();
        indicators.put("mineName", a.getMineName());
        indicators.put("mineType", a.getMineType());
        indicators.put("accidentType", a.getAccidentType());
        indicators.put("trappedOrInjuredMiners", a.getTrappedOrInjuredMiners());
        indicators.put("fatalities", a.getFatalities());
        indicators.put("rescueOperationsOngoing", a.getRescueOperationsOngoing());
        alertClient.notifyIncident("MINING", a.getId(), a.getWard(), a.getDistrict(), a.getProvince(),
                a.getSeverity(), a.getLatitude(), a.getLongitude(), indicators);
        return a;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MiningAccident accident, HttpServletRequest request) {
        Caller c = caller(request);

        if (c.ward() != null) {
            if (accident.getWard() == null || accident.getWard().isBlank()) {
                accident.setWard(c.ward());
            } else if (!c.ward().equalsIgnoreCase(accident.getWard())) {
                return forbidden("you can only capture incidents for your own ward (" + c.ward() + ")");
            }
        }
        accident.setReporter(c.username());

        return ResponseEntity.ok(raiseAlert(service.create(accident, c.username())));
    }

    @GetMapping
    public List<MiningAccident> getAll(HttpServletRequest request) {
        Caller c = caller(request);
        return service.findAll().stream().filter(a -> isVisible(a, c)).toList();
    }

    @GetMapping("/approved")
    public List<MiningAccident> getApproved() {
        return service.findByStatus(IncidentStatus.APPROVED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpServletRequest request) {
        MiningAccident accident = service.findById(id);
        if (!isVisible(accident, caller(request))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(accident);
    }

    /** Filter by status, still subject to the pending-record visibility rules. */
    @GetMapping("/status/{status}")
    public List<MiningAccident> getByStatus(@PathVariable IncidentStatus status, HttpServletRequest request) {
        Caller c = caller(request);
        return service.findByStatus(status).stream().filter(a -> isVisible(a, c)).toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody MiningAccident accident,
                                    HttpServletRequest request) {
        Caller c = caller(request);
        MiningAccident existing = service.findById(id);

        ResponseEntity<Map<String, String>> denied = checkRecorderCanModify(existing, c);
        if (denied != null) {
            return denied;
        }
        if (c.ward() != null && accident.getWard() != null
                && !c.ward().equalsIgnoreCase(accident.getWard())) {
            return forbidden("you can only capture incidents for your own ward (" + c.ward() + ")");
        }

        return ResponseEntity.ok(raiseAlert(service.update(id, accident, c.username())));
    }

    @PostMapping("/{id}/approve")
    public MiningAccident approve(@PathVariable Long id, HttpServletRequest request) {
        return service.approve(id, caller(request).username());
    }

    @PostMapping("/{id}/reject")
    public MiningAccident reject(@PathVariable Long id,
                                 @RequestBody Map<String, String> body,
                                 HttpServletRequest request) {
        return service.reject(id, body.get("reason"), caller(request).username());
    }

    @PostMapping("/{id}/request-correction")
    public MiningAccident requestCorrection(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        return service.requestCorrection(id, body.get("reason"), caller(request).username());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Caller c = caller(request);
        MiningAccident existing = service.findById(id);

        ResponseEntity<Map<String, String>> denied = checkRecorderCanModify(existing, c);
        if (denied != null) {
            return denied;
        }

        service.delete(id, c.username());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditLog>> getAudit(@PathVariable Long id, HttpServletRequest request) {
        MiningAccident accident = service.findById(id);
        if (!isVisible(accident, caller(request))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.getAuditLogs(id));
    }
}
