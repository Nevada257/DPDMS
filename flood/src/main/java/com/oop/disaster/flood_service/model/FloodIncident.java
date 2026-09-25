package com.oop.disaster.flood_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
public class FloodIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ward is required")
    private String ward;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Province is required")
    private String province;

    @NotBlank(message = "Occurrence date and time is required")
    private String occurrenceDateTime;

    @NotBlank(message = "Reporter is required")
    private String reporter;

    @NotBlank(message = "Severity is required")
    private String severity;

    @NotBlank(message = "Status is required")
    private String status;

    // Approval workflow
    private String approvalStatus = "PENDING";

    private String rejectionReason;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private double longitude;

    @DecimalMin(value = "0.0", message = "Peak water level cannot be negative")
    private double peakWaterLevel;

    @NotBlank(message = "River basin is required")
    private String riverBasin;

    @Min(value = 0, message = "Households displaced cannot be negative")
    private int householdsDisplaced;

    @DecimalMin(value = "0.0", message = "Area flooded cannot be negative")
    private double areaFlooded;

    @Min(value = 0, message = "Duration of inundation cannot be negative")
    private int durationOfInundation;

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

    public String getOccurrenceDateTime() {
        return occurrenceDateTime;
    }

    public void setOccurrenceDateTime(String occurrenceDateTime) {
        this.occurrenceDateTime = occurrenceDateTime;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getPeakWaterLevel() {
        return peakWaterLevel;
    }

    public void setPeakWaterLevel(double peakWaterLevel) {
        this.peakWaterLevel = peakWaterLevel;
    }

    public String getRiverBasin() {
        return riverBasin;
    }

    public void setRiverBasin(String riverBasin) {
        this.riverBasin = riverBasin;
    }

    public int getHouseholdsDisplaced() {
        return householdsDisplaced;
    }

    public void setHouseholdsDisplaced(int householdsDisplaced) {
        this.householdsDisplaced = householdsDisplaced;
    }

    public double getAreaFlooded() {
        return areaFlooded;
    }

    public void setAreaFlooded(double areaFlooded) {
        this.areaFlooded = areaFlooded;
    }

    public int getDurationOfInundation() {
        return durationOfInundation;
    }

    public void setDurationOfInundation(int durationOfInundation) {
        this.durationOfInundation = durationOfInundation;
    }
}