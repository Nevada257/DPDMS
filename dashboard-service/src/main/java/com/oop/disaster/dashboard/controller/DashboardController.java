package com.oop.disaster.dashboard.controller;

import com.oop.disaster.dashboard.security.AuthUser;
import com.oop.disaster.dashboard.service.DashboardService;
import com.oop.disaster.dashboard.service.DashboardService.Filters;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only dashboard API. Only APPROVED incidents are ever returned, and only
 * for the hazards within the caller's scope.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    /** Counts by hazard, severity and status; recent incidents; monthly trend; map points. */
    @GetMapping("/overview")
    public Map<String, Object> overview(@AuthenticationPrincipal AuthUser user,
                                        @RequestHeader("Authorization") String authorization,
                                        @RequestParam(required = false) String hazard,
                                        @RequestParam(required = false) String ward,
                                        @RequestParam(required = false) String district,
                                        @RequestParam(required = false) String severity,
                                        @RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to,
                                        @RequestParam(defaultValue = "12") int months) {
        return dashboard.overview(user, authorization,
                new Filters(hazard, ward, district, severity, from, to), months);
    }

    /**
     * Filtered list of approved incidents in the shared metadata shape.
     * Also used as the data source for reports.
     * Dates are yyyy-MM-dd and inclusive.
     */
    @GetMapping("/incidents")
    public Map<String, Object> incidents(@AuthenticationPrincipal AuthUser user,
                                         @RequestHeader("Authorization") String authorization,
                                         @RequestParam(required = false) String hazard,
                                         @RequestParam(required = false) String ward,
                                         @RequestParam(required = false) String district,
                                         @RequestParam(required = false) String severity,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to) {
        var data = dashboard.collect(user, authorization,
                new Filters(hazard, ward, district, severity, from, to));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", data.incidents().size());
        result.put("incidents", data.incidents());
        result.put("unavailable", data.unavailable());
        return result;
    }
}
