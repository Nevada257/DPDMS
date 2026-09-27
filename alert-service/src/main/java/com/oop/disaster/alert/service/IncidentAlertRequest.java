package com.oop.disaster.alert.service;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Incident summary posted by a hazard service after an incident is captured,
 * edited or approved. {@code indicators} carries the hazard-specific key
 * indicators (e.g. peakWaterLevel, active, fatalities) used by the alert rules.
 */
public record IncidentAlertRequest(
        @NotBlank String hazard,
        Long incidentId,
        String ward,
        String district,
        String province,
        String severity,
        Double latitude,
        Double longitude,
        Map<String, Object> indicators) {

    /** Numeric indicator, or 0 if absent / not a number. */
    public double number(String key) {
        Object v = indicators == null ? null : indicators.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    /** Boolean indicator (accepts true / "true"). */
    public boolean flag(String key) {
        Object v = indicators == null ? null : indicators.get(key);
        return v instanceof Boolean b ? b : v != null && "true".equalsIgnoreCase(v.toString());
    }

    /** Text indicator, or empty string. */
    public String text(String key) {
        Object v = indicators == null ? null : indicators.get(key);
        return v == null ? "" : v.toString();
    }
}
