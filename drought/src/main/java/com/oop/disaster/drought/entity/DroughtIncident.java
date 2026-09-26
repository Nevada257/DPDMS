package com.oop.disaster.drought.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "drought_incidents")
public class DroughtIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----- Shared incident metadata -----

    @NotBlank(message = "Ward is required")
    private String ward;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Province is required")
    private String province;

    @NotNull(message = "Date/time of occurrence is required")
    private LocalDateTime dateTimeOfOccurrence;

    @NotBlank(message = "Reporter is required")
    private String reporter;

    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL",
            message = "Severity must be LOW, MEDIUM, HIGH or CRITICAL")
    private String severity;

    private String status;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0",  message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
    private Double longitude;

    // ----- Drought-specific indicators -----

    @NotNull(message = "Rainfall deficit is required")
    @PositiveOrZero(message = "Rainfall deficit cannot be negative")
    private Double rainfallDeficitMm;

    @NotNull(message = "Consecutive dry days is required")
    @PositiveOrZero(message = "Consecutive dry days cannot be negative")
    private Integer consecutiveDryDays;

    @NotNull(message = "Crop failure percentage is required")
    @DecimalMin(value = "0.0",   message = "Crop failure cannot be below 0%")
    @DecimalMax(value = "100.0", message = "Crop failure cannot exceed 100%")
    private Double cropFailurePercentage;

    @NotNull(message = "People facing water shortages is required")
    @PositiveOrZero(message = "People facing water shortages cannot be negative")
    private Integer peopleFacingWaterShortages;

    @NotNull(message = "Livestock mortality count is required")
    @PositiveOrZero(message = "Livestock mortality cannot be negative")
    private Integer livestockMortalityCount;

    // ----- Workflow -----
    private String rejectionReason;

    // ----- Getters and Setters -----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDateTime getDateTimeOfOccurrence() { return dateTimeOfOccurrence; }
    public void setDateTimeOfOccurrence(LocalDateTime d) { this.dateTimeOfOccurrence = d; }

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

    public Double getRainfallDeficitMm() { return rainfallDeficitMm; }
    public void setRainfallDeficitMm(Double rainfallDeficitMm) { this.rainfallDeficitMm = rainfallDeficitMm; }

    public Integer getConsecutiveDryDays() { return consecutiveDryDays; }
    public void setConsecutiveDryDays(Integer consecutiveDryDays) { this.consecutiveDryDays = consecutiveDryDays; }

    public Double getCropFailurePercentage() { return cropFailurePercentage; }
    public void setCropFailurePercentage(Double cropFailurePercentage) { this.cropFailurePercentage = cropFailurePercentage; }

    public Integer getPeopleFacingWaterShortages() { return peopleFacingWaterShortages; }
    public void setPeopleFacingWaterShortages(Integer peopleFacingWaterShortages) { this.peopleFacingWaterShortages = peopleFacingWaterShortages; }

    public Integer getLivestockMortalityCount() { return livestockMortalityCount; }
    public void setLivestockMortalityCount(Integer livestockMortalityCount) { this.livestockMortalityCount = livestockMortalityCount; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}