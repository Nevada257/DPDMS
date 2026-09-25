package com.oop.disaster;

import com.oop.disaster.model.FireIncident;
import com.oop.disaster.model.IncidentStatus;
import com.oop.disaster.repository.AuditLogRepository;
import com.oop.disaster.repository.FireIncidentRepository;
import com.oop.disaster.service.AlertService;
import com.oop.disaster.service.FireIncidentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FireIncidentServiceTest {

    @Test
    void newIncidentStartsAsPending() {
        FireIncidentRepository repository = Mockito.mock(FireIncidentRepository.class);
        AuditLogRepository audit = Mockito.mock(AuditLogRepository.class);
        AlertService alerts = Mockito.mock(AlertService.class);

        FireIncident input = new FireIncident();
        Mockito.when(repository.save(Mockito.any(FireIncident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FireIncidentService service = new FireIncidentService(repository, audit, alerts);
        FireIncident result = service.create(input);

        assertEquals(IncidentStatus.PENDING, result.getStatus());
    }

    @Test
    void approvalChangesStatus() {
        FireIncidentRepository repository = Mockito.mock(FireIncidentRepository.class);
        AuditLogRepository audit = Mockito.mock(AuditLogRepository.class);
        AlertService alerts = Mockito.mock(AlertService.class);

        FireIncident incident = new FireIncident();
        incident.setStatus(IncidentStatus.PENDING);

        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(incident));
        Mockito.when(repository.save(Mockito.any(FireIncident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FireIncidentService service = new FireIncidentService(repository, audit, alerts);
        FireIncident result = service.changeStatus(
                1L, IncidentStatus.APPROVED, "Approved", "supervisor");

        assertEquals(IncidentStatus.APPROVED, result.getStatus());
    }
}
