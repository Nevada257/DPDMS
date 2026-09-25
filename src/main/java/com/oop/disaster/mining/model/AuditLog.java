package com.oop.disaster.mining.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long accidentId; private String actor; private String action;
    @Enumerated(EnumType.STRING) private IncidentStatus previousStatus;
    @Enumerated(EnumType.STRING) private IncidentStatus newStatus;
    private String details; private LocalDateTime timestamp = LocalDateTime.now();
    public AuditLog() {}
    public AuditLog(Long accidentId, String actor, String action, IncidentStatus prev, IncidentStatus newStatus, String details) {
        this.accidentId = accidentId; this.actor = actor; this.action = action;
        this.previousStatus = prev; this.newStatus = newStatus; this.details = details; this.timestamp = LocalDateTime.now();
    }
    public Long getId() { return id; } public Long getAccidentId() { return accidentId; } public void setAccidentId(Long a) { this.accidentId = a; }
    public String getActor() { return actor; } public void setActor(String a) { this.actor = a; }
    public String getAction() { return action; } public void setAction(String a) { this.action = a; }
    public IncidentStatus getPreviousStatus() { return previousStatus; } public void setPreviousStatus(IncidentStatus s) { this.previousStatus = s; }
    public IncidentStatus getNewStatus() { return newStatus; } public void setNewStatus(IncidentStatus s) { this.newStatus = s; }
    public String getDetails() { return details; } public void setDetails(String d) { this.details = d; }
    public LocalDateTime getTimestamp() { return timestamp; }
}