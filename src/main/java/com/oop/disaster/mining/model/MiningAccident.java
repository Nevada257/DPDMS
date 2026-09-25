package com.oop.disaster.mining.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mining_accident")
public class MiningAccident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ward is required")
    private String ward;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Province is required")
    private String province;

    @NotNull(message = "Occurrence date time is required")
    private LocalDateTime occurrenceDateTime;

    @NotBlank(message = "Reporter is required")
    private String reporter;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    private IncidentStatus status = IncidentStatus.PENDING;

    @NotNull
    @Min(value = -90, message = "Latitude must be >= -90")
    @Max(value = 90, message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull
    @Min(value = -180, message = "Longitude must be >= -180")
    @Max(value = 180, message = "Longitude must be <= 180")
    private Double longitude;

    @NotBlank(message = "Mine name is required")
    private String mineName;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MineType mineType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AccidentType accidentType;

    @NotNull
    @Min(value = 0, message = "Trapped/injured miners cannot be negative")
    private Integer trappedOrInjuredMiners = 0;

    @NotNull
    @Min(value = 0, message = "Fatalities cannot be negative")
    private Integer fatalities = 0;

    private Boolean rescueOperationsOngoing = false;

    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getMineName() { return mineName; }
    public void setMineName(String mineName) { this.mineName = mineName; }
    public MineType getMineType() { return mineType; }
    public void setMineType(MineType mineType) { this.mineType = mineType; }
    public AccidentType getAccidentType() { return accidentType; }
    public void setAccidentType(AccidentType accidentType) { this.accidentType = accidentType; }
    public Integer getTrappedOrInjuredMiners() { return trappedOrInjuredMiners; }
    public void setTrappedOrInjuredMiners(Integer trappedOrInjuredMiners) { this.trappedOrInjuredMiners = trappedOrInjuredMiners; }
    public Integer getFatalities() { return fatalities; }
    public void setFatalities(Integer fatalities) { this.fatalities = fatalities; }
    public Boolean getRescueOperationsOngoing() { return rescueOperationsOngoing; }
    public void setRescueOperationsOngoing(Boolean rescueOperationsOngoing) { this.rescueOperationsOngoing = rescueOperationsOngoing; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}