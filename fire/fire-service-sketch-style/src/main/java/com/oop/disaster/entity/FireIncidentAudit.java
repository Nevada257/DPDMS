package com.oop.disaster.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fire_incident_audit")
public class FireIncidentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long incidentId;
    private String action;
    private String performedBy;
    private LocalDateTime changedAt;
    private String reason;

    public FireIncidentAudit() {}

    public FireIncidentAudit(Long incidentId, String action, String performedBy, LocalDateTime changedAt, String reason) {
        this.incidentId = incidentId;
        this.action = action;
        this.performedBy = performedBy;
        this.changedAt = changedAt;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Long getIncidentId() { return incidentId; }
    public String getAction() { return action; }
    public String getPerformedBy() { return performedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getReason() { return reason; }
}
