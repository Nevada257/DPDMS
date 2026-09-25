package com.oop.disaster.drought;

import com.oop.disaster.drought.entity.DroughtIncident;
import com.oop.disaster.drought.repository.DroughtAuditTrailRepository;
import com.oop.disaster.drought.repository.DroughtIncidentRepository;
import com.oop.disaster.drought.service.DroughtIncidentService;
import com.oop.disaster.drought.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DroughtIncidentControllerTest {

    @Autowired private DroughtIncidentService service;
    @Autowired private DroughtIncidentRepository incidentRepo;
    @Autowired private DroughtAuditTrailRepository auditRepo;
    @Autowired private JwtService jwtService;

    private String recorderToken;
    private String supervisorToken;
    private String nationalToken;
    private String floodToken;

    @BeforeEach
    void setUp() {
        auditRepo.deleteAll();
        incidentRepo.deleteAll();

        recorderToken   = jwtService.generateToken("recorder1",   "DROUGHT_RECORDER",   "DROUGHT");
        supervisorToken = jwtService.generateToken("supervisor1", "DROUGHT_SUPERVISOR", "DROUGHT");
        nationalToken   = jwtService.generateToken("national1",   "NATIONAL_USER",      "ALL");
        floodToken      = jwtService.generateToken("floodrec",    "FLOOD_RECORDER",     "FLOOD");
    }

    private DroughtIncident sampleIncident() {
        DroughtIncident i = new DroughtIncident();
        i.setWard("Ward 5");
        i.setDistrict("Rushinga");
        i.setProvince("Mashonaland Central");
        i.setDateTimeOfOccurrence(java.time.LocalDateTime.now());
        i.setReporter("recorder1");
        i.setSeverity("HIGH");
        i.setLatitude(-16.78);
        i.setLongitude(31.96);
        i.setRainfallDeficitMm(120.0);
        i.setConsecutiveDryDays(45);
        i.setCropFailurePercentage(72.0);
        i.setPeopleFacingWaterShortages(350);
        i.setLivestockMortalityCount(18);
        return i;
    }

    // ---------- TOKEN / SCOPING (unit-level) ----------

    @Test
    void tokenGeneration_worksForAllRoles() {
        assertNotNull(recorderToken);
        assertNotNull(supervisorToken);
        assertNotNull(nationalToken);
        assertNotNull(floodToken);
        assertEquals("DROUGHT", jwtService.extractHazardScope(recorderToken));
        assertEquals("FLOOD",   jwtService.extractHazardScope(floodToken));
        assertEquals("ALL",     jwtService.extractHazardScope(nationalToken));
    }

    // ---------- SERVICE-LEVEL INTEGRATION ----------

    @Test
    void createdIncident_hasPendingStatus() {
        DroughtIncident saved = service.createIncident(sampleIncident());
        assertNotNull(saved.getId());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    void approveWorkflow_changesStatusAndWritesAudit() {
        DroughtIncident saved = service.createIncident(sampleIncident());
        DroughtIncident approved = service.approveIncident(saved.getId(), "supervisor1")
                .orElseThrow();
        assertEquals("APPROVED", approved.getStatus());
        assertEquals(1, auditRepo.findByIncidentIdOrderByPerformedAtAsc(saved.getId()).size());
    }

    @Test
    void cannotApproveTwice() {
        DroughtIncident saved = service.createIncident(sampleIncident());
        service.approveIncident(saved.getId(), "supervisor1");
        assertThrows(IllegalStateException.class,
                () -> service.approveIncident(saved.getId(), "supervisor1"));
    }

    @Test
    void rejectWorkflow_storesReason() {
        DroughtIncident saved = service.createIncident(sampleIncident());
        DroughtIncident rejected = service.rejectIncident(saved.getId(),
                "Not enough evidence", "supervisor1").orElseThrow();
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("Not enough evidence", rejected.getRejectionReason());
    }

    @Test
    void requestCorrection_movesToCorrectionRequested() {
        DroughtIncident saved = service.createIncident(sampleIncident());
        DroughtIncident updated = service.requestCorrection(saved.getId(),
                "Fix GPS", "supervisor1").orElseThrow();
        assertEquals("CORRECTION_REQUESTED", updated.getStatus());
    }
}