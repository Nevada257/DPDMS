package com.oop.disaster.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fire_incidents")
public class FireIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ward;
    private String district;
    private String province;
    private LocalDateTime occurrenceTime;
    private String reporter;
    private String reporterEmail;
    private String reporterPhone;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status = IncidentStatus.PENDING;

    private double latitude;
    private double longitude;
    private double areaBurned;

    @Enumerated(EnumType.STRING)
    private CauseType suspectedCause;

    private int injuriesOrFatalities;
    private int structuresDestroyed;
    private boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDateTime getOccurrenceTime() { return occurrenceTime; }
    public void setOccurrenceTime(LocalDateTime occurrenceTime) { this.occurrenceTime = occurrenceTime; }

    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }

    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }

    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAreaBurned() { return areaBurned; }
    public void setAreaBurned(double areaBurned) { this.areaBurned = areaBurned; }

    public CauseType getSuspectedCause() { return suspectedCause; }
    public void setSuspectedCause(CauseType suspectedCause) { this.suspectedCause = suspectedCause; }

    public int getInjuriesOrFatalities() { return injuriesOrFatalities; }
    public void setInjuriesOrFatalities(int injuriesOrFatalities) { this.injuriesOrFatalities = injuriesOrFatalities; }

    public int getStructuresDestroyed() { return structuresDestroyed; }
    public void setStructuresDestroyed(int structuresDestroyed) { this.structuresDestroyed = structuresDestroyed; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
