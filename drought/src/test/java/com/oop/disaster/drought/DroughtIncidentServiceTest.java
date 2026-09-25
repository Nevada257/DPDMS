package com.oop.disaster.drought;

import com.oop.disaster.drought.entity.DroughtIncident;
import com.oop.disaster.drought.repository.DroughtAuditTrailRepository;
import com.oop.disaster.drought.repository.DroughtIncidentRepository;
import com.oop.disaster.drought.service.DroughtIncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DroughtIncidentServiceTest {

    @Mock
    private DroughtIncidentRepository incidentRepository;

    @Mock
    private DroughtAuditTrailRepository auditRepository;

    @InjectMocks
    private DroughtIncidentService service;

    private DroughtIncident pending;

    @BeforeEach
    void setUp() {
        pending = new DroughtIncident();
        pending.setId(1L);
        pending.setStatus("PENDING");
        pending.setReporter("recorder1");
    }

    // ---------- createIncident ----------

    @Test
    void createIncident_shouldForceStatusToPending() {
        DroughtIncident incoming = new DroughtIncident();
        incoming.setStatus("APPROVED");  // caller tries to sneak this in
        incoming.setReporter("recorder1");

        when(incidentRepository.save(any(DroughtIncident.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DroughtIncident saved = service.createIncident(incoming);

        assertEquals("PENDING", saved.getStatus(),
                "Newly created incidents must always be PENDING");
        verify(auditRepository, times(1)).save(any());
    }

    // ---------- approveIncident ----------

    @Test
    void approveIncident_shouldChangeStatusToApproved() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(incidentRepository.save(any(DroughtIncident.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Optional<DroughtIncident> result = service.approveIncident(1L, "supervisor1");

        assertTrue(result.isPresent());
        assertEquals("APPROVED", result.get().getStatus());
        verify(auditRepository, times(1)).save(any());
    }

    @Test
    void approveIncident_shouldRejectAlreadyApprovedIncident() {
        pending.setStatus("APPROVED");
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThrows(IllegalStateException.class,
                () -> service.approveIncident(1L, "supervisor1"));
    }

    // ---------- rejectIncident ----------

    @Test
    void rejectIncident_shouldChangeStatusToRejected() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(incidentRepository.save(any(DroughtIncident.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Optional<DroughtIncident> result =
                service.rejectIncident(1L, "Insufficient evidence", "supervisor1");

        assertTrue(result.isPresent());
        assertEquals("REJECTED", result.get().getStatus());
        assertEquals("Insufficient evidence", result.get().getRejectionReason());
    }

    @Test
    void rejectIncident_shouldRejectNonPendingIncident() {
        pending.setStatus("APPROVED");
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThrows(IllegalStateException.class,
                () -> service.rejectIncident(1L, "reason", "supervisor1"));
    }

    // ---------- requestCorrection ----------

    @Test
    void requestCorrection_shouldMoveToCorrectionRequested() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(incidentRepository.save(any(DroughtIncident.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Optional<DroughtIncident> result =
                service.requestCorrection(1L, "Fix GPS coordinates", "supervisor1");

        assertTrue(result.isPresent());
        assertEquals("CORRECTION_REQUESTED", result.get().getStatus());
    }



}