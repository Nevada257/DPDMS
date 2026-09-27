package com.oop.disaster.controller;

import com.oop.disaster.JwtService;
import com.oop.disaster.model.FireIncident;
import com.oop.disaster.model.IncidentStatus;
import com.oop.disaster.service.FireIncidentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.oop.disaster.service.AlertClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fire incident REST API.
 *
 * Role rules (enforced here and in SecurityConfig, never only in the UI):
 *  - RECORDER   : creates incidents for their own ward only; sees approved
 *                 incidents plus their own submissions; edits/deletes only
 *                 their own, not-yet-approved records.
 *  - SUPERVISOR : (fire only - checked by JwtAuthenticationFilter) sees all
 *                 fire records and approves / rejects / requests corrections.
 *  - ADMIN      : provincial administrator - read only, may see pending records.
 *  - NATIONAL   : read only, approved records only.
 */
@RestController
@RequestMapping("/api/fire-incidents")
public class FireIncidentController {

    private final FireIncidentService service;
    private final JwtService jwtService;
    private final AlertClient alertClient;

    public FireIncidentController(FireIncidentService service, JwtService jwtService,
                                  AlertClient alertClient) {
        this.service = service;
        this.jwtService = jwtService;
        this.alertClient = alertClient;
    }

    /** Identity of the caller, taken from the signed JWT. */
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

    /** Pending / rejected records are visible only to their recorder, the supervisor and the admin. */
    private boolean isVisible(FireIncident incident, Caller c) {
        if (incident.getStatus() == IncidentStatus.APPROVED) {
            return true;
        }
        if (c.role() == null) {
            return false;
        }
        if (c.is("SUPERVISOR") || c.is("ADMIN")) {
            return true;
        }
        if (c.is("RECORDER")) {
            return c.username() != null && c.username().equalsIgnoreCase(incident.getReporter());
        }
        return false; // NATIONAL: approved only
    }

    /** Recorders may change only their own records, in their own ward, before approval. */
    private ResponseEntity<Map<String, String>> checkRecorderCanModify(FireIncident existing, Caller c) {
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

    /** Sends the incident to the alert-service, which decides whether it meets the alert criteria. */
    private FireIncident raiseAlert(FireIncident i) {
        Map<String, Object> indicators = new LinkedHashMap<>();
        indicators.put("areaBurned", i.getAreaBurned());
        indicators.put("suspectedCause", i.getSuspectedCause());
        indicators.put("injuriesOrFatalities", i.getInjuriesOrFatalities());
        indicators.put("structuresDestroyed", i.getStructuresDestroyed());
        indicators.put("active", i.isActive());
        alertClient.notifyIncident("FIRE", i.getId(), i.getWard(), i.getDistrict(), i.getProvince(),
                i.getSeverity(), i.getLatitude(), i.getLongitude(), indicators);
        return i;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FireIncident incident, HttpServletRequest request) {
        Caller c = caller(request);

        if (c.ward() != null) {
            if (incident.getWard() == null || incident.getWard().isBlank()) {
                incident.setWard(c.ward());
            } else if (!c.ward().equalsIgnoreCase(incident.getWard())) {
                return forbidden("you can only capture incidents for your own ward (" + c.ward() + ")");
            }
        }
        incident.setReporter(c.username());

        return ResponseEntity.ok(raiseAlert(service.create(incident, c.username())));
    }

    @GetMapping
    public List<FireIncident> getVisible(HttpServletRequest request) {
        Caller c = caller(request);
        return service.getAll().stream().filter(i -> isVisible(i, c)).toList();
    }

    /** Unfiltered listing - supervisor / admin only (locked down in SecurityConfig). */
    @GetMapping("/all")
    public List<FireIncident> getAll() {
        return service.getAll();
    }

    @GetMapping("/approved")
    public List<FireIncident> getApproved() {
        return service.getAll().stream()
                .filter(i -> i.getStatus() == IncidentStatus.APPROVED)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpServletRequest request) {
        FireIncident incident = service.getOne(id);
        if (!isVisible(incident, caller(request))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(incident);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody FireIncident incident,
                                    HttpServletRequest request) {
        Caller c = caller(request);
        FireIncident existing = service.getOne(id);

        ResponseEntity<Map<String, String>> denied = checkRecorderCanModify(existing, c);
        if (denied != null) {
            return denied;
        }
        if (c.ward() != null && incident.getWard() != null
                && !c.ward().equalsIgnoreCase(incident.getWard())) {
            return forbidden("you can only capture incidents for your own ward (" + c.ward() + ")");
        }

        return ResponseEntity.ok(raiseAlert(service.update(id, incident, c.username())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Caller c = caller(request);
        FireIncident existing = service.getOne(id);

        ResponseEntity<Map<String, String>> denied = checkRecorderCanModify(existing, c);
        if (denied != null) {
            return denied;
        }

        service.delete(id, c.username());
        return ResponseEntity.noContent().build();
    }

    // ---------------- Approval workflow (SUPERVISOR only, see SecurityConfig) ----------------

    @PutMapping("/{id}/approve")
    public FireIncident approve(@PathVariable Long id, HttpServletRequest request) {
        return service.changeStatus(id, IncidentStatus.APPROVED, "Approved", caller(request).username());
    }

    @PutMapping("/{id}/reject")
    public FireIncident reject(@PathVariable Long id,
                               @RequestParam String reason,
                               HttpServletRequest request) {
        return service.changeStatus(id, IncidentStatus.REJECTED, reason, caller(request).username());
    }

    @PutMapping("/{id}/correction")
    public FireIncident correction(@PathVariable Long id,
                                   @RequestParam String reason,
                                   HttpServletRequest request) {
        return service.changeStatus(id, IncidentStatus.NEEDS_CORRECTION, reason, caller(request).username());
    }
}
