package com.oop.disaster.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oop.disaster.dashboard.security.AuthUser;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Unit tests: hazard scoping, pending-record exclusion, normalisation and aggregation. */
class DashboardServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final AuthUser NATIONAL = new AuthUser("national_user", "NATIONAL", "ALL", null);
    private static final AuthUser FLOOD_SUPERVISOR = new AuthUser("flood_supervisor", "SUPERVISOR", "FLOOD", null);

    @Test
    void nationalUserSeesAllFiveHazards() {
        assertEquals(5, DashboardService.visibleHazards(NATIONAL, null).size());
    }

    @Test
    void supervisorSeesOnlyOwnHazard() {
        assertEquals(List.of(Hazard.FLOOD), DashboardService.visibleHazards(FLOOD_SUPERVISOR, null));
    }

    @Test
    void supervisorCannotRequestAnotherHazard() {
        assertTrue(DashboardService.visibleHazards(FLOOD_SUPERVISOR, "DROUGHT").isEmpty());
    }

    @Test
    void pendingRecordsAreDroppedEvenIfAServiceReturnsThem() throws Exception {
        var json = mapper.readTree("""
                [{"id":1,"ward":"Ward 1","status":"APPROVED","severity":"HIGH","latitude":-16.6,"longitude":32.2,
                  "occurrenceDateTime":"2026-09-01T10:00:00","diseaseName":"Anthrax","animalSpecies":"Cattle",
                  "confirmedHumanCases":2,"eventClassification":"CLUSTER"},
                 {"id":2,"ward":"Ward 1","status":"PENDING","severity":"LOW","latitude":-16.6,"longitude":32.2}]""");
        List<IncidentView> views = HazardClient.toApprovedViews(Hazard.ZOONOTIC, json);
        assertEquals(1, views.size());
        assertEquals(1L, views.get(0).id());
        assertEquals("CLUSTER", views.get(0).operationalStatus());
    }

    @Test
    void floodUsesApprovalStatusFieldAndIsoDate() throws Exception {
        var node = mapper.readTree("""
                {"id":5,"ward":"Ward 3","district":"Rushinga","approvalStatus":"APPROVED","status":"ongoing",
                 "severity":"critical","occurrenceDateTime":"2026-08-14T06:30","latitude":-16.7,"longitude":32.1,
                 "peakWaterLevel":4.2,"householdsDisplaced":35}""");
        IncidentView v = IncidentNormalizer.normalize(Hazard.FLOOD, node);
        assertEquals("APPROVED", v.approvalStatus());
        assertEquals("CRITICAL", v.severity());
        assertEquals("ONGOING", v.operationalStatus());
        assertEquals("2026-08", v.month());
        assertEquals(4.2, v.indicators().get("peakWaterLevel"));
    }

    @Test
    void jacksonArrayDatesAreConverted() throws Exception {
        var node = mapper.readTree("{\"occurrenceTime\":[2026,9,3,14,5,0]}");
        assertEquals("2026-09-03T14:05", IncidentNormalizer.date(node, "occurrenceTime"));
    }

    @Test
    void unavailableServiceIsReportedWithoutFailingTheDashboard() {
        HazardClient client = mock(HazardClient.class);
        when(client.fetchApproved(any(), anyString())).thenAnswer(inv -> {
            Hazard h = inv.getArgument(0);
            if (h == Hazard.FIRE) {
                return CompletableFuture.completedFuture(new HazardClient.Result(h, List.of(), "fire-service unavailable"));
            }
            return CompletableFuture.completedFuture(new HazardClient.Result(h, List.of(view(h, "2026-09-01")), null));
        });

        Map<String, Object> overview = new DashboardService(client).overview(NATIONAL, "Bearer x",
                new DashboardService.Filters(null, null, null, null, null, null), 12);

        assertEquals(4, overview.get("total"));
        assertTrue(((Map<?, ?>) overview.get("unavailable")).containsKey("FIRE"));
    }

    @Test
    void dateFiltersAreInclusive() {
        var f = new DashboardService.Filters(null, null, null, null, "2026-09-01", "2026-09-30");
        assertTrue(f.matches(view(Hazard.FLOOD, "2026-09-01")));
        assertTrue(f.matches(view(Hazard.FLOOD, "2026-09-30")));
        assertFalse(f.matches(view(Hazard.FLOOD, "2026-10-01")));
    }

    @Test
    void trendCountsIncidentsPerMonth() {
        String thisMonth = YearMonth.now().toString();
        var trend = DashboardService.trend(
                List.of(view(Hazard.FLOOD, thisMonth + "-02"), view(Hazard.FLOOD, thisMonth + "-10")),
                List.of(Hazard.FLOOD), 3);
        @SuppressWarnings("unchecked")
        Map<String, List<Long>> series = (Map<String, List<Long>>) trend.get("series");
        assertEquals(List.of(0L, 0L, 2L), series.get("FLOOD"));
    }

    private static IncidentView view(Hazard h, String day) {
        return new IncidentView(h, 1L, "Ward 1", "Rushinga", "Mashonaland Central", "HIGH", "APPROVED",
                "ACTIVE", day + "T08:00", -16.6, 32.2, "rec", "headline", Map.of());
    }
}
