package com.oop.disaster.service;

import com.oop.disaster.model.AlertLog;
import com.oop.disaster.model.FireIncident;
import com.oop.disaster.repository.AlertLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AlertService {

    private final AlertLogRepository alertLogRepository;
    private final EmailAlertService emailAlertService;
    private final WhatsAppAlertService whatsAppAlertService;

    public AlertService(AlertLogRepository alertLogRepository,
                        EmailAlertService emailAlertService,
                        WhatsAppAlertService whatsAppAlertService) {
        this.alertLogRepository = alertLogRepository;
        this.emailAlertService = emailAlertService;
        this.whatsAppAlertService = whatsAppAlertService;
    }

    @Async
    public void dispatchForFire(FireIncident incident) {
        if (!incident.isActive()) {
            return;
        }

        if (incident.getReporterEmail() != null && !incident.getReporterEmail().isBlank()) {
            try {
                emailAlertService.send(incident.getReporterEmail(),
                        "Active Fire Alert",
                        "An active fire has been reported in " + incident.getWard());
                saveLog(incident, "EMAIL", incident.getReporterEmail(), "SENT");
            } catch (Exception e) {
                saveLog(incident, "EMAIL", incident.getReporterEmail(), "FAILED");
            }
        }

        if (incident.getReporterPhone() != null && !incident.getReporterPhone().isBlank()) {
            try {
                whatsAppAlertService.send(incident.getReporterPhone(),
                        "Active fire reported in " + incident.getWard());
                saveLog(incident, "WHATSAPP", incident.getReporterPhone(), "SENT");
            } catch (Exception e) {
                saveLog(incident, "WHATSAPP", incident.getReporterPhone(), "FAILED");
            }
        }
    }

    private void saveLog(FireIncident incident, String channel,
                         String recipient, String status) {
        AlertLog log = new AlertLog();
        log.setIncidentId(incident.getId());
        log.setHazardType("FIRE");
        log.setChannel(channel);
        log.setRecipient(recipient);
        log.setSentAt(LocalDateTime.now());
        log.setDeliveryStatus(status);
        alertLogRepository.save(log);
    }
}
