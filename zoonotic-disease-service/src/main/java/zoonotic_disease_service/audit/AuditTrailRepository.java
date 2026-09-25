package zoonotic_disease_service.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditTrailRepository
        extends JpaRepository<AuditTrail, Long> {

    List<AuditTrail> findAllByOrderByPerformedAtDesc();

    List<AuditTrail> findByIncidentIdOrderByPerformedAtDesc(
            Long incidentId
    );
}