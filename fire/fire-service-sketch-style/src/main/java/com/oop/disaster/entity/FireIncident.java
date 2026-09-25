package com.oop.disaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "fire_incidents")
public class FireIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String ward;

    @NotBlank
    private String district;

    @NotBlank
    private String province;

    @NotNull
    private LocalDateTime occurrenceDateTime;

    @NotBlank
    private String reporter;

    @NotBlank
    private String severity;

    @NotBlank
    private String status = "PENDING";

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    @NotNull
    @DecimalMin("0.0")
    private Double areaBurnedHectares;

    @NotBlank
    private String suspectedCause;

    @NotNull
    @Min(0)
    private Integer injuries;

    @NotNull
    @Min(0)
    private Integer fatalities;

    @NotNull
    @Min(0)
    private Integer structuresDestroyed;

    @NotBlank
    private String fireStatus;

    public FireIncident() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public LocalDateTime getOccurrenceDateTime() { return occurrenceDateTime; }
    public void setOccurrenceDateTime(LocalDateTime occurrenceDateTime) { this.occurrenceDateTime = occurrenceDateTime; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getAreaBurnedHectares() { return areaBurnedHectares; }
    public void setAreaBurnedHectares(Double areaBurnedHectares) { this.areaBurnedHectares = areaBurnedHectares; }
    public String getSuspectedCause() { return suspectedCause; }
    public void setSuspectedCause(String suspectedCause) { this.suspectedCause = suspectedCause; }
    public Integer getInjuries() { return injuries; }
    public void setInjuries(Integer injuries) { this.injuries = injuries; }
    public Integer getFatalities() { return fatalities; }
    public void setFatalities(Integer fatalities) { this.fatalities = fatalities; }
    public Integer getStructuresDestroyed() { return structuresDestroyed; }
    public void setStructuresDestroyed(Integer structuresDestroyed) { this.structuresDestroyed = structuresDestroyed; }
    public String getFireStatus() { return fireStatus; }
    public void setFireStatus(String fireStatus) { this.fireStatus = fireStatus; }
}
