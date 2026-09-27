package com.oop.disaster.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps each hazard service's JSON into {@link IncidentView}. The hazard
 * services were built independently, so field names differ slightly
 * (e.g. approvalStatus vs status, occurrenceTime vs dateTimeOfOccurrence);
 * this class is the single place that knows those differences.
 */
public final class IncidentNormalizer {

    private IncidentNormalizer() {
    }

    public static IncidentView normalize(Hazard hazard, JsonNode n) {
        return switch (hazard) {
            case FLOOD -> build(hazard, n, text(n, "approvalStatus"),
                    upperOr(text(n, "status"), "REPORTED"), date(n, "occurrenceDateTime"),
                    "Peak water " + num(n, "peakWaterLevel") + " m, "
                            + num(n, "householdsDisplaced") + " households displaced",
                    indicators(n, "peakWaterLevel", "riverBasin", "householdsDisplaced",
                            "areaFlooded", "durationOfInundation"));
            case DROUGHT -> build(hazard, n, text(n, "status"), "ONGOING", date(n, "dateTimeOfOccurrence"),
                    num(n, "cropFailurePercentage") + "% crop failure, "
                            + num(n, "consecutiveDryDays") + " dry days",
                    indicators(n, "rainfallDeficitMm", "consecutiveDryDays", "cropFailurePercentage",
                            "peopleFacingWaterShortages", "livestockMortalityCount"));
            case FIRE -> build(hazard, n, text(n, "status"),
                    n.path("active").asBoolean(false) ? "ACTIVE" : "CONTAINED", date(n, "occurrenceTime"),
                    num(n, "areaBurned") + " ha burned, " + num(n, "injuriesOrFatalities") + " casualties",
                    indicators(n, "areaBurned", "suspectedCause", "injuriesOrFatalities",
                            "structuresDestroyed", "active"));
            case ZOONOTIC -> build(hazard, n, text(n, "status"),
                    upperOr(text(n, "eventClassification"), "REPORTED"), date(n, "occurrenceDateTime"),
                    text(n, "diseaseName") + " in " + text(n, "animalSpecies") + ", "
                            + num(n, "confirmedHumanCases") + " human cases",
                    indicators(n, "diseaseName", "animalSpecies", "confirmedHumanCases",
                            "confirmedAnimalCases", "eventClassification"));
            case MINING -> build(hazard, n, text(n, "status"),
                    n.path("rescueOperationsOngoing").asBoolean(false) ? "RESCUE_ONGOING" : "RESCUE_COMPLETED",
                    date(n, "occurrenceDateTime"),
                    text(n, "mineName") + ": " + num(n, "fatalities") + " fatalities, "
                            + num(n, "trappedOrInjuredMiners") + " trapped/injured",
                    indicators(n, "mineName", "mineType", "accidentType",
                            "trappedOrInjuredMiners", "fatalities", "rescueOperationsOngoing"));
        };
    }

    private static IncidentView build(Hazard hazard, JsonNode n, String approval, String operational,
                                      String occurredAt, String headline, Map<String, Object> indicators) {
        return new IncidentView(
                hazard,
                n.hasNonNull("id") ? n.get("id").asLong() : null,
                text(n, "ward"),
                text(n, "district"),
                text(n, "province"),
                upperOr(text(n, "severity"), "UNKNOWN"),
                upperOr(approval, "UNKNOWN"),
                operational,
                occurredAt,
                n.hasNonNull("latitude") ? n.get("latitude").asDouble() : null,
                n.hasNonNull("longitude") ? n.get("longitude").asDouble() : null,
                text(n, "reporter"),
                headline,
                indicators);
    }

    private static Map<String, Object> indicators(JsonNode n, String... names) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String name : names) {
            JsonNode v = n.get(name);
            if (v == null || v.isNull()) {
                out.put(name, null);
            } else if (v.isBoolean()) {
                out.put(name, v.asBoolean());
            } else if (v.isIntegralNumber()) {
                out.put(name, v.asLong());
            } else if (v.isNumber()) {
                out.put(name, v.asDouble());
            } else {
                out.put(name, v.asText());
            }
        }
        return out;
    }

    static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String num(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return "?";
        }
        double d = v.asDouble();
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private static String upperOr(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s.trim().toUpperCase();
    }

    /** Accepts ISO strings or Jackson's array form [yyyy, M, d, H, m, s]. */
    static String date(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isArray() && v.size() >= 3) {
            return String.format("%04d-%02d-%02dT%02d:%02d",
                    v.get(0).asInt(), v.get(1).asInt(), v.get(2).asInt(),
                    v.size() > 3 ? v.get(3).asInt() : 0, v.size() > 4 ? v.get(4).asInt() : 0);
        }
        return v.asText();
    }
}
