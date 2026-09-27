package com.oop.disaster.alert;

import com.oop.disaster.alert.service.AlertRules;
import com.oop.disaster.alert.service.IncidentAlertRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for the alerting criteria of every hazard. */
class AlertRulesTest {

    private final AlertRules rules = new AlertRules(3.0, 50);

    private static IncidentAlertRequest req(String hazard, String severity, Map<String, Object> indicators) {
        return new IncidentAlertRequest(hazard, 1L, "Ward 1", "Rushinga", "Mashonaland Central",
                severity, -16.6, 32.2, indicators);
    }

    @Test
    void floodAboveDangerLevelTriggers() {
        assertTrue(rules.evaluate(req("FLOOD", "LOW", Map.of("peakWaterLevel", 3.4))).isPresent());
    }

    @Test
    void floodBelowDangerLevelWithLowSeverityDoesNotTrigger() {
        assertTrue(rules.evaluate(req("FLOOD", "LOW", Map.of("peakWaterLevel", 1.2))).isEmpty());
    }

    @Test
    void floodHighSeverityTriggersEvenBelowDangerLevel() {
        assertTrue(rules.evaluate(req("FLOOD", "HIGH", Map.of("peakWaterLevel", 1.2))).isPresent());
    }

    @Test
    void activeFireTriggers() {
        assertTrue(rules.evaluate(req("FIRE", "LOW", Map.of("active", true))).isPresent());
    }

    @Test
    void containedFireDoesNotTrigger() {
        assertTrue(rules.evaluate(req("FIRE", "HIGH", Map.of("active", false))).isEmpty());
    }

    @Test
    void zoonoticClusterTriggers() {
        var result = rules.evaluate(req("ZOONOTIC", "MEDIUM",
                Map.of("eventClassification", "CLUSTER", "diseaseName", "Anthrax", "confirmedHumanCases", 0)));
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("Anthrax"));
    }

    @Test
    void miningWithFatalitiesTriggers() {
        assertTrue(rules.evaluate(req("MINING", "HIGH",
                Map.of("fatalities", 2, "trappedOrInjuredMiners", 0))).isPresent());
    }

    @Test
    void miningWithoutCasualtiesDoesNotTrigger() {
        assertTrue(rules.evaluate(req("MINING", "LOW",
                Map.of("fatalities", 0, "trappedOrInjuredMiners", 0))).isEmpty());
    }

    @Test
    void droughtCropFailureAboveThresholdTriggers() {
        assertTrue(rules.evaluate(req("DROUGHT", "MEDIUM", Map.of("cropFailurePercentage", 72.0))).isPresent());
    }

    @Test
    void unknownHazardNeverTriggers() {
        assertTrue(rules.evaluate(req("VOLCANO", "CRITICAL", Map.of())).isEmpty());
    }

    @Test
    void numericIndicatorsSentAsStringsAreAccepted() {
        assertTrue(rules.evaluate(req("FLOOD", "LOW", Map.of("peakWaterLevel", "4.1"))).isPresent());
    }
}
