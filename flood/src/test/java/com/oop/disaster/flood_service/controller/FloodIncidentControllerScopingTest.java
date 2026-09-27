package com.oop.disaster.flood_service.controller;

import com.oop.disaster.flood_service.AlertClient;
import com.oop.disaster.flood_service.JwtService;
import com.oop.disaster.flood_service.model.FloodIncident;
import com.oop.disaster.flood_service.service.FloodIncidentService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Ward-level scoping and pending-record visibility in the flood API
 * (the hazard-level check is covered by HazardScopingFilterTest).
 */
class FloodIncidentControllerScopingTest {

    static final String SECRET = "test-secret-key-for-dpdms-unit-tests-0123456789";

    private FloodIncidentService service;
    private AlertClient alertClient;
    private FloodIncidentController controller;

    @BeforeEach
    void setUp() {
        service = mock(FloodIncidentService.class);
        alertClient = mock(AlertClient.class);
        controller = new FloodIncidentController(service, new JwtService(SECRET), alertClient);
    }

    static MockHttpServletRequest as(String user, String role, String ward) {
        String token = Jwts.builder()
                .subject(user).claim("role", role).claim("hazardScope", role.equals("NATIONAL") ? "ALL" : "FLOOD")
                .claim("ward", ward)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    static FloodIncident incident(long id, String ward, String reporter, String approvalStatus) {
        FloodIncident i = new FloodIncident();
        i.setId(id);
        i.setWard(ward);
        i.setReporter(reporter);
        i.setApprovalStatus(approvalStatus);
        return i;
    }

    @Test
    void recorderCannotCaptureForAnotherWard() {
        ResponseEntity<?> r = controller.createIncident(incident(0, "Ward 7", "x", null),
                as("flood_recorder", "RECORDER", "Ward 1"));
        assertEquals(403, r.getStatusCode().value());
        verify(service, never()).createIncident(any(), any());
    }

    @Test
    void recorderCapturesInOwnWardAndBecomesReporter() {
        when(service.createIncident(any(), eq("flood_recorder")))
                .thenAnswer(inv -> inv.getArgument(0));
        FloodIncident submitted = incident(0, "Ward 1", "someone-else", null);

        ResponseEntity<?> r = controller.createIncident(submitted, as("flood_recorder", "RECORDER", "Ward 1"));

        assertEquals(200, r.getStatusCode().value());
        assertEquals("flood_recorder", submitted.getReporter(), "reporter comes from the token, not the body");
        verify(alertClient).notifyIncident(eq("FLOOD"), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void nationalUserSeesOnlyApprovedRecords() {
        when(service.getAllIncidents()).thenReturn(List.of(
                incident(1, "Ward 1", "flood_recorder", "APPROVED"),
                incident(2, "Ward 1", "flood_recorder", "PENDING")));

        List<FloodIncident> visible = controller.getAllIncidents(as("national_user", "NATIONAL", null));

        assertEquals(List.of(1L), visible.stream().map(FloodIncident::getId).toList());
    }

    @Test
    void recorderSeesOwnPendingButNotOthers() {
        when(service.getAllIncidents()).thenReturn(List.of(
                incident(1, "Ward 1", "flood_recorder", "PENDING"),
                incident(2, "Ward 1", "other_recorder", "PENDING"),
                incident(3, "Ward 2", "other_recorder", "APPROVED")));

        List<FloodIncident> visible = controller.getAllIncidents(as("flood_recorder", "RECORDER", "Ward 1"));

        assertEquals(List.of(1L, 3L), visible.stream().map(FloodIncident::getId).toList());
    }

    @Test
    void supervisorAndAdminSeePendingRecords() {
        when(service.getAllIncidents()).thenReturn(List.of(incident(2, "Ward 1", "r", "PENDING")));
        assertEquals(1, controller.getAllIncidents(as("flood_supervisor", "SUPERVISOR", null)).size());
        assertEquals(1, controller.getAllIncidents(as("provincial_admin", "ADMIN", null)).size());
    }

    @Test
    void pendingRecordByIdIsHiddenFromNationalUser() {
        when(service.getIncidentById(2L)).thenReturn(Optional.of(incident(2, "Ward 1", "r", "PENDING")));
        assertEquals(404, controller.getIncidentById(2L, as("national_user", "NATIONAL", null))
                .getStatusCode().value());
    }

    @Test
    void recorderCannotEditSomeoneElsesRecord() {
        when(service.getIncidentById(5L)).thenReturn(Optional.of(incident(5, "Ward 1", "other_recorder", "PENDING")));
        ResponseEntity<?> r = controller.updateIncident(5L, incident(5, "Ward 1", null, null),
                as("flood_recorder", "RECORDER", "Ward 1"));
        assertEquals(403, r.getStatusCode().value());
        verify(service, never()).updateIncident(anyLong(), any(), any());
    }

    @Test
    void recorderCannotEditApprovedRecord() {
        when(service.getIncidentById(5L)).thenReturn(Optional.of(incident(5, "Ward 1", "flood_recorder", "APPROVED")));
        ResponseEntity<?> r = controller.updateIncident(5L, incident(5, "Ward 1", null, null),
                as("flood_recorder", "RECORDER", "Ward 1"));
        assertEquals(403, r.getStatusCode().value());
    }

    @Test
    void recorderCannotDeleteRecordFromAnotherWard() {
        when(service.getIncidentById(9L)).thenReturn(Optional.of(incident(9, "Ward 2", "flood_recorder", "PENDING")));
        assertEquals(403, controller.deleteIncident(9L, as("flood_recorder", "RECORDER", "Ward 1"))
                .getStatusCode().value());
        verify(service, never()).deleteIncident(anyLong(), any());
    }

    @Test
    void approvalIsAttributedToTheAuthenticatedSupervisor() {
        when(service.approveIncident(3L, "flood_supervisor")).thenReturn(incident(3, "Ward 1", "r", "APPROVED"));
        assertEquals(200, controller.approveIncident(3L, as("flood_supervisor", "SUPERVISOR", null))
                .getStatusCode().value());
        verify(service).approveIncident(3L, "flood_supervisor");
    }
}
