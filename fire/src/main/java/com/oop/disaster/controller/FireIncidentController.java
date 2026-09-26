package com.oop.disaster.controller;

import com.oop.disaster.JwtService;
import com.oop.disaster.model.FireIncident;
import com.oop.disaster.service.FireIncidentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fire-incidents")
public class FireIncidentController {

    private final FireIncidentService service;
    private final JwtService jwtService;

    public FireIncidentController(FireIncidentService service, JwtService jwtService) {
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

    private boolean isVisible(FireIncident incident, String role, String username) {
        if (incident.getStatus() != null && incident.getStatus().name().equals("APPROVED")) {
            return true;
        }
        if ("SUPERVISOR".equalsIgnoreCase(role)) {
            return true;
        }
        if ("NATIONAL".equalsIgnoreCase(role)) {
            return false;
        }
        return username != null && username.equalsIgnoreCase(incident.getReporter());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FireIncident incident, HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);
            String username = jwtService.extractUsername(token);

            if ("RECORDER".equalsIgnoreCase(role)) {

                if (ward != null
                        && incident.getWard() != null
                        && !ward.equalsIgnoreCase(incident.getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: you can only capture incidents for your own ward (" + ward + ")")
                    );
                }

                incident.setReporter(username);
            }
        }

        return ResponseEntity.ok(service.create(incident));
    }

    // Default listing — visibility-filtered (this replaces the old blanket "hide all pending" behavior)
    @GetMapping
    public List<FireIncident> getVisible(HttpServletRequest request) {

        String token = extractToken(request);
        List<FireIncident> all = service.getAll();

        if (token == null) {
            return all.stream().filter(i -> i.getStatus() != null && i.getStatus().name().equals("APPROVED")).toList();
        }

        String role = jwtService.extractRole(token);
        String username = jwtService.extractUsername(token);

        return all.stream().filter(i -> isVisible(i, role, username)).toList();
    }

    // Unfiltered listing — supervisor only (locked down in SecurityConfig)
    @GetMapping("/all")
    public List<FireIncident> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpServletRequest request) {

        FireIncident incident = service.getOne(id);

        String token = extractToken(request);
        String role = token != null ? jwtService.extractRole(token) : null;
        String username = token != null ? jwtService.extractUsername(token) : null;

        if (!isVisible(incident, role, username)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(incident);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @RequestBody FireIncident incident,
                                     HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role) && ward != null) {
                FireIncident existing = service.getOne(id);

                if (existing.getWard() != null
                        && !ward.equalsIgnoreCase(existing.getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: this incident belongs to a different ward")
                    );
                }
            }
        }

        return ResponseEntity.ok(service.update(id, incident));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role) && ward != null) {
                FireIncident existing = service.getOne(id);

                if (existing.getWard() != null
                        && !ward.equalsIgnoreCase(existing.getWard())) {

                    return ResponseEntity.status(403).body(
                            Map.of("error", "Forbidden: this incident belongs to a different ward")
                    );
                }
            }
        }

        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}