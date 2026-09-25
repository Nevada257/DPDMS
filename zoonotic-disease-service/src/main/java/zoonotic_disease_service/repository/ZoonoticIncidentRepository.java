package zoonotic_disease_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zoonotic_disease_service.entity.ZoonoticIncident;
import zoonotic_disease_service.enums.IncidentStatus;

import java.util.List;

public interface ZoonoticIncidentRepository
        extends JpaRepository<ZoonoticIncident, Long> {

    List<ZoonoticIncident> findByWardAndStatus(
            String ward,
            IncidentStatus status
    );

    List<ZoonoticIncident> findByWardAndCreatedByUsernameAndStatusNot(
            String ward,
            String createdByUsername,
            IncidentStatus status
    );

    List<ZoonoticIncident> findByProvince(
            String province
    );

    List<ZoonoticIncident> findByProvinceAndStatus(
            String province,
            IncidentStatus status
    );
}