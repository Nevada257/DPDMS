package com.oop.disaster.alert.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** One delivery attempt of one alert, on one channel, to one recipient. */
@Entity
@Table(name = "alert_log", indexes = @Index(name = "idx_alert_hazard", columnList = "hazard"))
public class AlertLog {

    public enum Channel { EMAIL, WHATSAPP }

    /** SENT = delivered to the provider; SIMULATED = no credentials configured; FAILED = provider error. */
    public enum DeliveryStatus { SENT, SIMULATED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String hazard;

    private Long incidentId;
    private String ward;
    private String district;
    private String severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(nullable = false)
    private String recipient;

    @Column(length = 1000)
    private String message;

    /** Why the incident met the alerting criteria. */
    private String triggerReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus deliveryStatus;

    @Column(length = 500)
    private String errorMessage;

    /** Who captured / acted on the incident that triggered the alert. */
    private String triggeredBy;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    public Long getId() { return id; }
    public String getHazard() { return hazard; }
    public void setHazard(String hazard) { this.hazard = hazard; }
    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTriggerReason() { return triggerReason; }
    public void setTriggerReason(String triggerReason) { this.triggerReason = triggerReason; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
