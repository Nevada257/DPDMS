package zoonotic_disease_service.audit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-trails")
public class AuditTrailController {

    private final AuditTrailRepository auditTrailRepository;

    public AuditTrailController(
            AuditTrailRepository auditTrailRepository) {

        this.auditTrailRepository = auditTrailRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuditTrail>> getAllAuditTrails() {

        return ResponseEntity.ok(
                auditTrailRepository
                        .findAllByOrderByPerformedAtDesc()
        );
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<List<AuditTrail>> getIncidentAuditTrail(
            @PathVariable Long incidentId) {

        return ResponseEntity.ok(
                auditTrailRepository
                        .findByIncidentIdOrderByPerformedAtDesc(
                                incidentId
                        )
        );
    }
}