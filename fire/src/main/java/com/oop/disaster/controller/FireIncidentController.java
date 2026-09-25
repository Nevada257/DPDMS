package com.oop.disaster.controller;

import com.oop.disaster.model.FireIncident;
import com.oop.disaster.service.FireIncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fire-incidents")
public class FireIncidentController {

    private final FireIncidentService service;

    public FireIncidentController(FireIncidentService service) {
        this.service = service;
    }

    @PostMapping
    public FireIncident create(@RequestBody FireIncident incident) {
        return service.create(incident);
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
    public FireIncident update(@PathVariable Long id,
                               @RequestBody FireIncident incident) {
        return service.update(id, incident);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
