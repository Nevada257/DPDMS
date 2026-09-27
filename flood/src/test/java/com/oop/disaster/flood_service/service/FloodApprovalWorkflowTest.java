package com.oop.disaster.flood_service.service;

import com.oop.disaster.flood_service.model.FloodAuditLog;
import com.oop.disaster.flood_service.model.FloodIncident;
import com.oop.disaster.flood_service.repository.FloodAuditLogRepository;
import com.oop.disaster.flood_service.repository.FloodIncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for the flood approval workflow and its audit trail. */
class FloodApprovalWorkflowTest {

    private FloodIncidentRepository repo;
    private FloodAuditLogRepository audit;
    private FloodIncidentService service;

    @BeforeEach
    void setUp() {
        repo = mock(FloodIncidentRepository.class);
        audit = mock(FloodAuditLogRepository.class);
        service = new FloodIncidentService(repo, audit);
        when(repo.save(any(FloodIncident.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private FloodIncident withStatus(String status) {
        FloodIncident i = new FloodIncident();
        i.setId(1L);
        i.setApprovalStatus(status);
        when(repo.findById(1L)).thenReturn(Optional.of(i));
        return i;
    }

    @Test
    void newIncidentIsAlwaysPending() {
        FloodIncident i = new FloodIncident();
        i.setApprovalStatus("APPROVED"); // client tries to skip approval
        assertEquals("PENDING", service.createIncident(i, "flood_recorder").getApprovalStatus());
    }

    @Test
    void approvePendingRecordsTheActor() {
        withStatus("PENDING");
        assertEquals("APPROVED", service.approveIncident(1L, "flood_supervisor").getApprovalStatus());

        ArgumentCaptor<FloodAuditLog> entry = ArgumentCaptor.forClass(FloodAuditLog.class);
        verify(audit).save(entry.capture());
        assertEquals("flood_supervisor", entry.getValue().getPerformedBy());
        assertEquals("APPROVED", entry.getValue().getAction());
    }

    @Test
    void cannotApproveTwice() {
        withStatus("APPROVED");
        assertThrows(RuntimeException.class, () -> service.approveIncident(1L, "flood_supervisor"));
    }

    @Test
    void rejectionRequiresAReason() {
        withStatus("PENDING");
        assertThrows(RuntimeException.class, () -> service.rejectIncident(1L, " ", "flood_supervisor"));
    }

    @Test
    void correctionsThenEditResubmitsAsPending() {
        withStatus("PENDING");
        service.requestCorrections(1L, "Fix GPS", "flood_supervisor");

        FloodIncident edited = new FloodIncident();
        FloodIncident result = service.updateIncident(1L, edited, "flood_recorder");

        assertEquals("PENDING", result.getApprovalStatus());
        assertNull(result.getRejectionReason());
    }

    @Test
    void editCannotChangeApprovalStatus() {
        withStatus("PENDING");
        FloodIncident edited = new FloodIncident();
        edited.setApprovalStatus("APPROVED");
        assertEquals("PENDING", service.updateIncident(1L, edited, "flood_recorder").getApprovalStatus());
    }
}
