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

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FireIncident incident, HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {
            String role = jwtService.extractRole(token);
            String ward = jwtService.extractWard(token);

            if ("RECORDER".equalsIgnoreCase(role)
                    && ward != null
                    && incident.getWard() != null
                    && !ward.equalsIgnoreCase(incident.getWard())) {

                return ResponseEntity.status(403).body(
                        Map.of("error", "Forbidden: you can only capture incidents for your own ward (" + ward + ")")
                );
            }
        }

        return ResponseEntity.ok(service.create(incident));
    }

    @GetMapping
    public List<FireIncident> getVisible() {
        return service.getAllVisible();
    }

    @GetMapping("/all")
    public List<FireIncident> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public FireIncident getOne(@PathVariable Long id) {
        return service.getOne(id);
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