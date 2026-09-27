package com.oop.disaster.dashboard.service;

import com.oop.disaster.dashboard.security.AuthUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Builds the dashboard from approved incidents.
 * Hazard scoping: a user with scope ALL (national user, provincial admin)
 * sees all five hazards; everyone else sees only their own hazard.
 * A hazard service that is down is reported in "unavailable" instead of
 * failing the whole dashboard.
 */
@Service
public class DashboardService {

    /** Optional filters, shared by the dashboard, map and report data feed. */
    public record Filters(String hazard, String ward, String district, String severity,
                          String from, String to) {
        boolean matches(IncidentView i) {
            return eq(ward, i.ward()) && eq(district, i.district()) && eq(severity, i.severity())
                    && (from == null || from.isBlank() || i.day().compareTo(from) >= 0)
                    && (to == null || to.isBlank() || (!i.day().isEmpty() && i.day().compareTo(to) <= 0));
        }

        private static boolean eq(String filter, String value) {
            return filter == null || filter.isBlank() || filter.equalsIgnoreCase(value);
        }
    }

    public record Collected(List<IncidentView> incidents, Map<String, String> unavailable) {
    }

    private final HazardClient client;

    public DashboardService(HazardClient client) {
        this.client = client;
    }

    /** Hazards this user may see on the dashboard. */
    public static List<Hazard> visibleHazards(AuthUser user, String requestedHazard) {
        List<Hazard> allowed = "ALL".equalsIgnoreCase(user.hazardScope())
                ? List.of(Hazard.values())
                : Arrays.stream(Hazard.values()).filter(h -> h.name().equalsIgnoreCase(user.hazardScope())).toList();

        if (requestedHazard == null || requestedHazard.isBlank()) {
            return allowed;
        }
        return allowed.stream().filter(h -> h.name().equalsIgnoreCase(requestedHazard)).toList();
    }

    public Collected collect(AuthUser user, String authorizationHeader, Filters filters) {
        List<CompletableFuture<HazardClient.Result>> calls = visibleHazards(user, filters.hazard()).stream()
                .map(h -> client.fetchApproved(h, authorizationHeader))
                .toList();

        List<IncidentView> incidents = new ArrayList<>();
        Map<String, String> unavailable = new LinkedHashMap<>();
        for (CompletableFuture<HazardClient.Result> call : calls) {
            HazardClient.Result r = call.join();
            if (r.ok()) {
                r.incidents().stream().filter(filters::matches).forEach(incidents::add);
            } else {
                unavailable.put(r.hazard().name(), r.error());
            }
        }
        incidents.sort(Comparator.comparing(IncidentView::occurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return new Collected(incidents, unavailable);
    }

    /** Counts by hazard / severity / status, recent incidents, monthly trend and map points. */
    public Map<String, Object> overview(AuthUser user, String authorizationHeader, Filters filters, int months) {
        Collected data = collect(user, authorizationHeader, filters);
        List<IncidentView> all = data.incidents();
        List<Hazard> hazards = visibleHazards(user, filters.hazard());

        Map<String, Long> byHazard = new LinkedHashMap<>();
        hazards.forEach(h -> byHazard.put(h.name(), 0L));
        all.forEach(i -> byHazard.merge(i.hazard().name(), 1L, Long::sum));

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (String s : List.of("LOW", "MEDIUM", "HIGH", "CRITICAL")) {
            bySeverity.put(s, 0L);
        }
        all.forEach(i -> bySeverity.merge(i.severity(), 1L, Long::sum));

        Map<String, Long> byStatus = all.stream().collect(Collectors.groupingBy(
                IncidentView::operationalStatus, TreeMap::new, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedAt", LocalDateTime.now().toString());
        result.put("hazards", hazards.stream().map(Enum::name).toList());
        result.put("total", all.size());
        result.put("byHazard", byHazard);
        result.put("bySeverity", bySeverity);
        result.put("byStatus", byStatus);
        result.put("recent", all.stream().limit(10).toList());
        result.put("trend", trend(all, hazards, months));
        result.put("mapPoints", all.stream().filter(i -> i.latitude() != null && i.longitude() != null).toList());
        result.put("unavailable", data.unavailable());
        return result;
    }

    /** Incidents per month per hazard for the last {@code months} months (oldest first). */
    static Map<String, Object> trend(List<IncidentView> incidents, List<Hazard> hazards, int months) {
        int span = Math.max(1, Math.min(months, 36));
        YearMonth now = YearMonth.now();
        List<String> labels = new ArrayList<>();
        for (int k = span - 1; k >= 0; k--) {
            labels.add(now.minusMonths(k).toString());
        }

        Map<String, List<Long>> series = new LinkedHashMap<>();
        for (Hazard h : hazards) {
            List<Long> counts = new ArrayList<>(Collections.nCopies(span, 0L));
            for (IncidentView i : incidents) {
                int idx = labels.indexOf(i.month());
                if (i.hazard() == h && idx >= 0) {
                    counts.set(idx, counts.get(idx) + 1);
                }
            }
            series.put(h.name(), counts);
        }
        return Map.of("months", labels, "series", series);
    }
}
