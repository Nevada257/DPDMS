package zoonotic_disease_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import zoonotic_disease_service.enums.EventClassification;
import zoonotic_disease_service.enums.IncidentStatus;
import zoonotic_disease_service.enums.Severity;

import java.time.LocalDateTime;

@Entity
@Table(name = "zoonotic_incidents")
public class ZoonoticIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ward is required")
    private String ward;

    private String district;

    private String province;

    @NotNull(message = "Occurrence date and time is required")
    private LocalDateTime occurrenceDateTime;

    private String reporter;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Severity is required")
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    @NotNull(message = "Latitude is required")
    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotBlank(message = "Disease name is required")
    private String diseaseName;

    @NotBlank(message = "Animal species is required")
    private String animalSpecies;

    @NotNull(message = "Confirmed human cases is required")
    @PositiveOrZero(message = "Confirmed human cases cannot be negative")
    private Integer confirmedHumanCases;

    @NotNull(message = "Confirmed animal cases is required")
    @PositiveOrZero(message = "Confirmed animal cases cannot be negative")
    private Integer confirmedAnimalCases;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Event classification is required")
    private EventClassification eventClassification;

    // NEW:
    // Stores the username of the person who created the incident
    private String createdByUsername;

    public ZoonoticIncident() {
    }

    public ZoonoticIncident(
            String ward,
            String district,
            String province,
            LocalDateTime occurrenceDateTime,
            String reporter,
            Severity severity,
            IncidentStatus status,
            Double latitude,
            Double longitude,
            String diseaseName,
            String animalSpecies,
            Integer confirmedHumanCases,
            Integer confirmedAnimalCases,
            EventClassification eventClassification) {

        this.ward = ward;
        this.district = district;
        this.province = province;
        this.occurrenceDateTime = occurrenceDateTime;
        this.reporter = reporter;
        this.severity = severity;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.diseaseName = diseaseName;
        this.animalSpecies = animalSpecies;
        this.confirmedHumanCases = confirmedHumanCases;
        this.confirmedAnimalCases = confirmedAnimalCases;
        this.eventClassification = eventClassification;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public LocalDateTime getOccurrenceDateTime() {
        return occurrenceDateTime;
    }

    public void setOccurrenceDateTime(LocalDateTime occurrenceDateTime) {
        this.occurrenceDateTime = occurrenceDateTime;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    public String getAnimalSpecies() {
        return animalSpecies;
    }

    public void setAnimalSpecies(String animalSpecies) {
        this.animalSpecies = animalSpecies;
    }

    public Integer getConfirmedHumanCases() {
        return confirmedHumanCases;
    }

    public void setConfirmedHumanCases(Integer confirmedHumanCases) {
        this.confirmedHumanCases = confirmedHumanCases;
    }

    public Integer getConfirmedAnimalCases() {
        return confirmedAnimalCases;
    }

    public void setConfirmedAnimalCases(Integer confirmedAnimalCases) {
        this.confirmedAnimalCases = confirmedAnimalCases;
    }

    public EventClassification getEventClassification() {
        return eventClassification;
    }

    public void setEventClassification(
            EventClassification eventClassification) {
        this.eventClassification = eventClassification;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }
}