package zoonotic_disease_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import zoonotic_disease_service.entity.ZoonoticIncident;
import zoonotic_disease_service.service.AlertClient;
import zoonotic_disease_service.service.ZoonoticIncidentService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/zoonotic-incidents")
@SecurityRequirement(name = "bearerAuth")
public class ZoonoticIncidentController {

    private final ZoonoticIncidentService service;
    private final AlertClient alertClient;

    public ZoonoticIncidentController(ZoonoticIncidentService service, AlertClient alertClient) {
        this.service = service;
        this.alertClient = alertClient;
    }

    /** Sends the incident to the alert-service, which decides whether it meets the alert criteria. */
    private ZoonoticIncident raiseAlert(ZoonoticIncident i) {
        Map<String, Object> indicators = new LinkedHashMap<>();
        indicators.put("diseaseName", i.getDiseaseName());
        indicators.put("animalSpecies", i.getAnimalSpecies());
        indicators.put("confirmedHumanCases", i.getConfirmedHumanCases());
        indicators.put("confirmedAnimalCases", i.getConfirmedAnimalCases());
        indicators.put("eventClassification", i.getEventClassification());
        alertClient.notifyIncident("ZOONOTIC", i.getId(), i.getWard(), i.getDistrict(), i.getProvince(),
                i.getSeverity(), i.getLatitude(), i.getLongitude(), indicators);
        return i;
    }

    @PostMapping
    public ResponseEntity<ZoonoticIncident> createIncident(
            @Valid @RequestBody ZoonoticIncident incident) {

        return ResponseEntity.ok(
                raiseAlert(service.createIncident(incident))
        );
    }

    @GetMapping
    public ResponseEntity<List<ZoonoticIncident>> getAllIncidents() {

        return ResponseEntity.ok(
                service.getAllIncidents()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZoonoticIncident> getIncidentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.getIncidentById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZoonoticIncident> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody ZoonoticIncident incident) {

        return ResponseEntity.ok(
                raiseAlert(service.updateIncident(id, incident))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable Long id) {

        service.deleteIncident(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ZoonoticIncident> approveIncident(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.approveIncident(id)
        );
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ZoonoticIncident> rejectIncident(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.rejectIncident(id)
        );
    }

    @PostMapping("/{id}/request-correction")
    public ResponseEntity<ZoonoticIncident> requestCorrection(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.requestCorrection(id)
        );
    }
}