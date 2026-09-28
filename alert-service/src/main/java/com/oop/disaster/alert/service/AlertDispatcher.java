package com.oop.disaster.alert.service;

import com.oop.disaster.alert.model.AlertLog;
import com.oop.disaster.alert.model.AlertLog.Channel;
import com.oop.disaster.alert.model.AlertLog.DeliveryStatus;
import com.oop.disaster.alert.model.Subscriber;
import com.oop.disaster.alert.repository.AlertLogRepository;
import com.oop.disaster.alert.repository.SubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends an alert to every active subscriber of the hazard, by email and by
 * WhatsApp, and logs every delivery (channel, recipient, time, status, attempts).
 *
 * Reliability:
 *  - runs on the "alertExecutor" thread pool, so the caller is never blocked;
 *  - a failed send is retried (default 3 attempts, 2 s then 4 s apart);
 *  - WhatsApp only delivers free text to numbers that messaged the business in the
 *    last 24 hours; outside that window the approved template message is sent instead;
 *  - WhatsApp goes through Green API when GREENAPI_* is configured (no 24-hour
 *    window, no daily token), otherwise through the Meta WhatsApp Cloud API.
 */
@Service
public class AlertDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AlertDispatcher.class);

    /** One delivery attempt. */
    @FunctionalInterface
    interface Attempt {
        void run() throws Exception;
    }

    /** Outcome after retrying: null error = delivered. */
    record Outcome(int attempts, Exception error) {
        boolean delivered() { return error == null; }
    }

    private final SubscriberRepository subscribers;
    private final AlertLogRepository logs;
    private final EmailSender email;
    private final WhatsAppSender whatsApp;
    private final GreenApiWhatsAppSender greenApi;
    private final int maxAttempts;
    private final long backoffMs;

    @Autowired
    public AlertDispatcher(SubscriberRepository subscribers, AlertLogRepository logs,
                           EmailSender email, WhatsAppSender whatsApp, GreenApiWhatsAppSender greenApi,
                           @Value("${alerts.retry.max-attempts:3}") int maxAttempts,
                           @Value("${alerts.retry.backoff-ms:2000}") long backoffMs) {
        this.subscribers = subscribers;
        this.logs = logs;
        this.email = email;
        this.whatsApp = whatsApp;
        this.greenApi = greenApi;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoffMs = Math.max(0, backoffMs);
    }

    /** Alert for a real incident: only subscribers of that hazard receive it. */
    @Async("alertExecutor")
    public void dispatch(IncidentAlertRequest incident, String reason, String triggeredBy) {
        List<Subscriber> recipients = subscribers.findAll().stream()
                .filter(s -> s.wants(incident.hazard()))
                .toList();
        deliver(incident, reason, triggeredBy, recipients);
    }

    /** Test alert from the provincial administrator: every active subscriber receives it. */
    @Async("alertExecutor")
    public void dispatchTest(String triggeredBy) {
        IncidentAlertRequest test = new IncidentAlertRequest("TEST", null, "Ward 1", "Rushinga",
                "Mashonaland Central", "LOW", -16.62, 32.21, java.util.Map.of());
        List<Subscriber> recipients = subscribers.findAll().stream().filter(Subscriber::isActive).toList();
        deliver(test, "test alert sent by " + triggeredBy + " to confirm email and WhatsApp delivery",
                triggeredBy, recipients);
    }

    void deliver(IncidentAlertRequest incident, String reason, String triggeredBy, List<Subscriber> recipients) {
        String subject = "DPDMS ALERT: " + incident.hazard().toUpperCase() + " in " + incident.ward()
                + (incident.district() == null ? "" : ", " + incident.district());
        String text = buildMessage(incident, reason);

        log.info("Dispatching {} alert for incident {} to {} subscriber(s): {}",
                incident.hazard(), incident.incidentId(), recipients.size(), reason);

        for (Subscriber s : recipients) {
            if (s.getEmail() != null && !s.getEmail().isBlank()) {
                sendEmail(incident, reason, triggeredBy, s.getEmail(), subject, text);
            }
            if (s.getPhone() != null && !s.getPhone().isBlank()) {
                sendWhatsApp(incident, reason, triggeredBy, s.getPhone(), subject, text);
            }
        }
    }

    private void sendEmail(IncidentAlertRequest i, String reason, String by,
                           String to, String subject, String text) {
        if (!email.isConfigured()) {
            record(i, reason, by, Channel.EMAIL, to, text, DeliveryStatus.SIMULATED, 0,
                    "MAIL_HOST not configured - email not sent");
            return;
        }
        Outcome o = withRetry(() -> email.send(to, subject, text), false);
        if (o.delivered()) {
            record(i, reason, by, Channel.EMAIL, to, text, DeliveryStatus.SENT, o.attempts(), null);
        } else {
            log.warn("Email alert to {} failed after {} attempt(s): {}", to, o.attempts(), o.error().getMessage());
            record(i, reason, by, Channel.EMAIL, to, text, DeliveryStatus.FAILED, o.attempts(), o.error().getMessage());
        }
    }

    private void sendWhatsApp(IncidentAlertRequest i, String reason, String by, String to,
                              String subject, String text) {
        if (greenApi.isConfigured()) {
            Outcome g = withRetry(() -> greenApi.send(to, subject, text), false);
            if (g.delivered()) {
                record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.SENT, g.attempts(), "via Green API");
            } else {
                log.warn("Green API WhatsApp alert to {} failed after {} attempt(s): {}",
                        to, g.attempts(), g.error().getMessage());
                record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.FAILED, g.attempts(),
                        "Green API: " + g.error().getMessage());
            }
            return;
        }
        if (!whatsApp.isConfigured()) {
            record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.SIMULATED, 0,
                    "No WhatsApp provider configured (GREENAPI_* or WHATSAPP_TOKEN) - message not sent");
            return;
        }
        Outcome o = withRetry(() -> whatsApp.send(to, text), true);
        if (o.delivered()) {
            record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.SENT, o.attempts(), null);
            return;
        }
        if (WhatsAppSender.isOutsideConversationWindow(o.error())) {
            Outcome t = withRetry(() -> whatsApp.sendTemplate(to), false);
            if (t.delivered()) {
                record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.SENT, o.attempts() + t.attempts(),
                        "Recipient outside WhatsApp's 24-hour window: delivered as the approved template message");
                return;
            }
            o = new Outcome(o.attempts() + t.attempts(), t.error());
        }
        log.warn("WhatsApp alert to {} failed after {} attempt(s): {}", to, o.attempts(), o.error().getMessage());
        record(i, reason, by, Channel.WHATSAPP, to, text, DeliveryStatus.FAILED, o.attempts(), o.error().getMessage());
    }

    /**
     * Runs the attempt up to maxAttempts times, waiting backoffMs, then twice that, between tries.
     * A WhatsApp "outside the 24-hour window" error is not transient, so it is not retried.
     */
    Outcome withRetry(Attempt attempt, boolean stopOnWindowError) {
        Exception last = null;
        for (int n = 1; n <= maxAttempts; n++) {
            try {
                attempt.run();
                return new Outcome(n, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Outcome(n, e);
            } catch (Exception e) {
                last = e;
                if (stopOnWindowError && WhatsAppSender.isOutsideConversationWindow(e)) {
                    return new Outcome(n, e);
                }
                if (n < maxAttempts && backoffMs > 0) {
                    try {
                        Thread.sleep(backoffMs * n);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return new Outcome(n, ie);
                    }
                }
            }
        }
        return new Outcome(maxAttempts, last);
    }

    private void record(IncidentAlertRequest i, String reason, String by, Channel channel, String recipient,
                        String text, DeliveryStatus status, int attempts, String note) {
        AlertLog entry = new AlertLog();
        entry.setHazard(i.hazard().toUpperCase());
        entry.setIncidentId(i.incidentId());
        entry.setWard(i.ward());
        entry.setDistrict(i.district());
        entry.setSeverity(i.severity());
        entry.setChannel(channel);
        entry.setRecipient(recipient);
        entry.setMessage(text.length() > 1000 ? text.substring(0, 1000) : text);
        entry.setTriggerReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
        entry.setDeliveryStatus(status);
        entry.setAttempts(attempts);
        entry.setErrorMessage(note == null || note.length() <= 500 ? note : note.substring(0, 500));
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
