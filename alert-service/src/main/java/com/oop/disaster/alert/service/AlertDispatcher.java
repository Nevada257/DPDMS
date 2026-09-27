package com.oop.disaster.alert.service;

import com.oop.disaster.alert.model.AlertLog;
import com.oop.disaster.alert.model.AlertLog.Channel;
import com.oop.disaster.alert.model.AlertLog.DeliveryStatus;
import com.oop.disaster.alert.model.Subscriber;
import com.oop.disaster.alert.repository.AlertLogRepository;
import com.oop.disaster.alert.repository.SubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends an alert to every active subscriber of the hazard, by email and by
 * WhatsApp, and logs every attempt (channel, recipient, time, delivery status).
 * Runs on the "alertExecutor" thread pool, so the caller is never blocked.
 */
@Service
public class AlertDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AlertDispatcher.class);

    private final SubscriberRepository subscribers;
    private final AlertLogRepository logs;
    private final EmailSender email;
    private final WhatsAppSender whatsApp;

    public AlertDispatcher(SubscriberRepository subscribers, AlertLogRepository logs,
                           EmailSender email, WhatsAppSender whatsApp) {
        this.subscribers = subscribers;
        this.logs = logs;
        this.email = email;
        this.whatsApp = whatsApp;
    }

    @Async("alertExecutor")
    public void dispatch(IncidentAlertRequest incident, String reason, String triggeredBy) {
        String subject = "DPDMS ALERT: " + incident.hazard().toUpperCase() + " in " + incident.ward()
                + (incident.district() == null ? "" : ", " + incident.district());
        String text = buildMessage(incident, reason);

        List<Subscriber> recipients = subscribers.findAll().stream()
                .filter(s -> s.wants(incident.hazard()))
                .toList();

        log.info("Dispatching {} alert for incident {} to {} subscriber(s): {}",
                incident.hazard(), incident.incidentId(), recipients.size(), reason);

        for (Subscriber s : recipients) {
            if (s.getEmail() != null && !s.getEmail().isBlank()) {
                sendEmail(incident, reason, triggeredBy, s.getEmail(), subject, text);
            }
            if (s.getPhone() != null && !s.getPhone().isBlank()) {
                sendWhatsApp(incident, reason, triggeredBy, s.getPhone(), text);
            }
        }
    }

    private void sendEmail(IncidentAlertRequest i, String reason, String by,
                           String to, String subject, String text) {
        if (!email.isConfigured()) {
            record(i, reason, by, Channel.EMAIL, to, text, DeliveryStatus.SIMULATED,
                    "MAIL_HOST not configured - email not sent");
            return;
        }
        try {
            email.send(to, subject, text);
            record(i, reason, by, Channel.EMAIL, to, text, DeliveryStatus.SENT, null);
        } catch (Exception e) {
            log.warn("Email alert to {} failed: {}", to, e.getMessage());
            record(i, reason, by, Channel.EMAIL, to, text, DeliveryStatus.FAILED, e.getMessage());
        }
    }

    private void sendWhatsApp(IncidentAlertRequest i, String reason, String by, String to, String text) {
        if (!whatsApp.isConfigured()) {
            record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.SIMULATED,
                    "WHATSAPP_TOKEN not configured - message not sent");
            return;
        }
        try {
            whatsApp.send(to, text);
            record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.SENT, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.FAILED, "Interrupted");
        } catch (Exception e) {
            log.warn("WhatsApp alert to {} failed: {}", to, e.getMessage());
            record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.FAILED, e.getMessage());
        }
    }

    private void record(IncidentAlertRequest i, String reason, String by, Channel channel, String recipient,
                        String text, DeliveryStatus status, String error) {
        AlertLog entry = new AlertLog();
        entry.setHazard(i.hazard().toUpperCase());
        entry.setIncidentId(i.incidentId());
        entry.setWard(i.ward());
        entry.setDistrict(i.district());
        entry.setSeverity(i.severity());
        entry.setChannel(channel);
        entry.setRecipient(recipient);
        entry.setMessage(text.length() > 1000 ? text.substring(0, 1000) : text);
        entry.setTriggerReason(reason);
        entry.setDeliveryStatus(status);
        entry.setErrorMessage(error == null || error.length() <= 500 ? error : error.substring(0, 500));
        entry.setTriggeredBy(by);
        entry.setSentAt(LocalDateTime.now());
        logs.save(entry);
    }

    static String buildMessage(IncidentAlertRequest i, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("DPDMS ").append(i.hazard().toUpperCase()).append(" ALERT\n");
        sb.append("Location: ").append(i.ward());
        if (i.district() != null) sb.append(", ").append(i.district());
        if (i.province() != null) sb.append(", ").append(i.province());
        sb.append('\n');
        if (i.severity() != null) sb.append("Severity: ").append(i.severity()).append('\n');
        sb.append("Reason: ").append(reason).append('\n');
        if (i.latitude() != null && i.longitude() != null) {
            sb.append("GPS: ").append(i.latitude()).append(", ").append(i.longitude())
              .append(" (https://maps.google.com/?q=").append(i.latitude()).append(',').append(i.longitude()).append(")\n");
        }
        if (i.incidentId() != null) sb.append("Incident #").append(i.incidentId());
        return sb.toString();
    }
}
