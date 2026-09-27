package zoonotic_disease_service.enums;

public enum Role {

    WARD_RECORDER,
    PROVINCIAL_SUPERVISOR,
    NATIONAL_USER,
    // Provincial administrator: may view pending records (read only)
    PROVINCIAL_ADMIN
}