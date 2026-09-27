package com.oop.disaster.alert.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The alerting criteria for every hazard, kept in one place.
 * Returns the reason an incident triggers an alert, or empty if it does not.
 *
 *  FLOOD    : peak water level at or above the danger level, or severity HIGH/CRITICAL
 *  DROUGHT  : crop failure at or above the threshold, or severity HIGH/CRITICAL
 *  FIRE     : fire still active (not contained)
 *  ZOONOTIC : classified as a cluster or an outbreak, or any confirmed human case
 *  MINING   : any fatalities or trapped / injured miners (casualties)
 */
@Component
public class AlertRules {

    private final double floodDangerLevelM;
    private final double droughtCropFailurePct;

    public AlertRules(@Value("${alerts.rules.flood-danger-level-m:3.0}") double floodDangerLevelM,
                      @Value("${alerts.rules.drought-crop-failure-pct:50}") double droughtCropFailurePct) {
        this.floodDangerLevelM = floodDangerLevelM;
        this.droughtCropFailurePct = droughtCropFailurePct;
    }

    public Optional<String> evaluate(IncidentAlertRequest r) {
        String hazard = r.hazard() == null ? "" : r.hazard().toUpperCase();
        List<String> reasons = new ArrayList<>();

        switch (hazard) {
            case "FLOOD" -> {
                double level = r.number("peakWaterLevel");
                if (level >= floodDangerLevelM) {
                    reasons.add("peak water level " + level + " m is at or above the danger level of "
                            + floodDangerLevelM + " m");
                }
                addIfSevere(r, reasons);
            }
            case "DROUGHT" -> {
                double crop = r.number("cropFailurePercentage");
                if (crop >= droughtCropFailurePct) {
                    reasons.add("crop failure " + crop + "% is at or above " + droughtCropFailurePct + "%");
                }
                addIfSevere(r, reasons);
            }
            case "FIRE" -> {
                if (r.flag("active")) {
                    reasons.add("fire is still burning");
                }
            }
            case "ZOONOTIC" -> {
                String classification = r.text("eventClassification").toUpperCase();
                if (classification.equals("CLUSTER") || classification.equals("OUTBREAK")) {
                    reasons.add(r.text("diseaseName") + " " + classification.toLowerCase() + " reported");
                }
                if (r.number("confirmedHumanCases") > 0) {
                    reasons.add((int) r.number("confirmedHumanCases") + " confirmed human case(s)");
                }
            }
            case "MINING" -> {
                int fatalities = (int) r.number("fatalities");
                int trapped = (int) r.number("trappedOrInjuredMiners");
                if (fatalities > 0 || trapped > 0) {
                    reasons.add("casualties: " + fatalities + " fatalities, " + trapped + " trapped or injured");
                }
            }
            default -> {
                return Optional.empty();
            }
        }
        return reasons.isEmpty() ? Optional.empty() : Optional.of(String.join("; ", reasons));
    }

    private static void addIfSevere(IncidentAlertRequest r, List<String> reasons) {
        String sev = r.severity() == null ? "" : r.severity().toUpperCase();
        if (sev.equals("HIGH") || sev.equals("CRITICAL")) {
            reasons.add("severity " + sev);
        }
    }
}
