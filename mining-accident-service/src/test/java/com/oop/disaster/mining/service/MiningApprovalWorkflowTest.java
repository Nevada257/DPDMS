package com.oop.disaster.mining.service;

import com.oop.disaster.mining.model.AuditLog;
import com.oop.disaster.mining.model.IncidentStatus;
import com.oop.disaster.mining.model.MiningAccident;
import com.oop.disaster.mining.repository.AuditLogRepository;
import com.oop.disaster.mining.repository.MiningAccidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for the mining approval workflow and its audit trail. */
class MiningApprovalWorkflowTest {

    private MiningAccidentRepository repo;
    private AuditLogRepository audit;
    private MiningAccidentService service;

    @BeforeEach
    void setUp() {
        repo = mock(MiningAccidentRepository.class);
        audit = mock(AuditLogRepository.class);
        service = new MiningAccidentService(repo, audit);
        when(repo.save(any(MiningAccident.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MiningAccident withStatus(IncidentStatus status) {
        MiningAccident a = new MiningAccident();
        a.setStatus(status);
        when(repo.findById(1L)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void newAccidentIsPending() {
        MiningAccident a = new MiningAccident();
        a.setStatus(IncidentStatus.APPROVED);
        assertEquals(IncidentStatus.PENDING, service.create(a, "mining_recorder").getStatus());
    }

    @Test
    void approvalIsAuditedWithTheActor() {
        withStatus(IncidentStatus.PENDING);
        service.approve(1L, "mining_supervisor");

        ArgumentCaptor<AuditLog> entry = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(entry.capture());
        assertEquals("mining_supervisor", entry.getValue().getActor());
        assertEquals(IncidentStatus.PENDING, entry.getValue().getPreviousStatus());
        assertEquals(IncidentStatus.APPROVED, entry.getValue().getNewStatus());
    }

    @Test
    void onlyPendingCanBeApproved() {
        withStatus(IncidentStatus.REJECTED);
        assertThrows(IllegalStateException.class, () -> service.approve(1L, "mining_supervisor"));
    }

    @Test
    void rejectRequiresReason() {
        withStatus(IncidentStatus.PENDING);
        assertThrows(IllegalArgumentException.class, () -> service.reject(1L, "", "mining_supervisor"));
    }

    @Test
    void editAfterCorrectionRequestResubmits() {
        withStatus(IncidentStatus.PENDING);
        service.requestCorrection(1L, "Wrong GPS", "mining_supervisor");
        MiningAccident result = service.update(1L, new MiningAccident(), "mining_recorder");
        assertEquals(IncidentStatus.PENDING, result.getStatus());
        assertNull(result.getRejectionReason());
    }
}
