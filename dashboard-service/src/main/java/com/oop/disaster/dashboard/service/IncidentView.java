package com.oop.disaster.dashboard.service;

import java.util.Map;

/**
 * An incident from any hazard service, in the shared metadata shape used by
 * the dashboard, the map and reports.
 *
 * @param operationalStatus hazard-specific live state, e.g. ACTIVE / CONTAINED for fires
 * @param occurredAt        ISO-8601 date-time of occurrence
 * @param headline          one-line summary of the key indicators, for map pop-ups
 * @param indicators        the hazard's five key indicators
 */
public record IncidentView(
        Hazard hazard,
        Long id,
        String ward,
        String district,
        String province,
        String severity,
        String approvalStatus,
        String operationalStatus,
        String occurredAt,
        Double latitude,
        Double longitude,
        String reporter,
        String headline,
        Map<String, Object> indicators) {

    /** "yyyy-MM" of the occurrence date, or "unknown". */
    public String month() {
        return occurredAt != null && occurredAt.length() >= 7 ? occurredAt.substring(0, 7) : "unknown";
    }

    /** "yyyy-MM-dd" of the occurrence date, or empty string. */
    public String day() {
        return occurredAt != null && occurredAt.length() >= 10 ? occurredAt.substring(0, 10) : "";
    }
}
