package com.oop.disaster.alert.service;

import com.oop.disaster.alert.model.AlertLog;
import com.oop.disaster.alert.model.AlertLog.DeliveryStatus;
import com.oop.disaster.alert.model.Subscriber;
import com.oop.disaster.alert.repository.AlertLogRepository;
import com.oop.disaster.alert.repository.SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for delivery reliability: retries, the WhatsApp template fallback and logging. */
class AlertDispatcherReliabilityTest {

    private SubscriberRepository subscribers;
    private AlertLogRepository logs;
    private EmailSender email;
    private WhatsAppSender whatsApp;
    private AlertDispatcher dispatcher;

    private final IncidentAlertRequest fire = new IncidentAlertRequest("FIRE", 7L, "Ward 1", "Rushinga",
            "Mashonaland Central", "HIGH", -16.62, 32.21, Map.of("active", true));

    @BeforeEach
    void setUp() {
        subscribers = mock(SubscriberRepository.class);
        logs = mock(AlertLogRepository.class);
        email = mock(EmailSender.class);
        whatsApp = mock(WhatsAppSender.class);
        when(email.isConfigured()).thenReturn(true);
        when(whatsApp.isConfigured()).thenReturn(true);
        dispatcher = new AlertDispatcher(subscribers, logs, email, whatsApp, 3, 0);
    }

    private List<AlertLog> savedLogs(int expected) {
        ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(logs, times(expected)).save(captor.capture());
        return captor.getAllValues();
    }

    private static Subscriber officer() {
        return new Subscriber("Duty Officer", "officer@example.org", "+263771234567", "ALL");
    }

    @Test
    void emailIsRetriedAndLoggedAsSentWithAttemptCount() {
        doThrow(new RuntimeException("SMTP timeout"))
                .doThrow(new RuntimeException("SMTP timeout"))
                .doNothing()
                .when(email).send(anyString(), anyString(), anyString());

        dispatcher.deliver(fire, "fire is still burning", "fire_recorder",
                List.of(new Subscriber("Only email", "officer@example.org", null, "ALL")));

        AlertLog entry = savedLogs(1).get(0);
        assertEquals(DeliveryStatus.SENT, entry.getDeliveryStatus());
        assertEquals(3, entry.getAttempts());
        assertEquals("officer@example.org", entry.getRecipient());
    }

    @Test
    void emailFailingEveryAttemptIsLoggedAsFailedWithTheReason() {
        doThrow(new RuntimeException("Authentication failed")).when(email).send(anyString(), anyString(), anyString());

        dispatcher.deliver(fire, "fire is still burning", "fire_recorder",
                List.of(new Subscriber("Only email", "officer@example.org", null, "ALL")));

        AlertLog entry = savedLogs(1).get(0);
        assertEquals(DeliveryStatus.FAILED, entry.getDeliveryStatus());
        assertEquals(3, entry.getAttempts());
        assertTrue(entry.getErrorMessage().contains("Authentication failed"));
    }

    @Test
    void whatsAppOutsideTheWindowFallsBackToTheTemplate() throws Exception {
        doThrow(new IOException("WhatsApp API returned 400: {\"error\":{\"code\":131047,\"message\":\"Re-engagement message\"}}"))
                .when(whatsApp).send(anyString(), anyString());

        dispatcher.deliver(fire, "fire is still burning", "fire_recorder",
                List.of(new Subscriber("Only phone", null, "+263771234567", "ALL")));

        verify(whatsApp, times(1)).send(anyString(), anyString()); // window errors are not retried
        verify(whatsApp).sendTemplate("+263771234567");
        AlertLog entry = savedLogs(1).get(0);
        assertEquals(DeliveryStatus.SENT, entry.getDeliveryStatus());
        assertTrue(entry.getErrorMessage().contains("template"));
    }

    @Test
    void bothChannelsAreLoggedSeparately() throws Exception {
        dispatcher.deliver(fire, "fire is still burning", "fire_recorder", List.of(officer()));

        List<AlertLog> entries = savedLogs(2);
        assertEquals(AlertLog.Channel.EMAIL, entries.get(0).getChannel());
        assertEquals(AlertLog.Channel.WHATSAPP, entries.get(1).getChannel());
        assertTrue(entries.stream().allMatch(e -> e.getDeliveryStatus() == DeliveryStatus.SENT));
        verify(whatsApp).send(eq("+263771234567"), contains("DPDMS FIRE ALERT"));
    }

    @Test
    void unconfiguredProvidersAreLoggedAsSimulated() {
        when(email.isConfigured()).thenReturn(false);
        when(whatsApp.isConfigured()).thenReturn(false);

        dispatcher.deliver(fire, "fire is still burning", "fire_recorder", List.of(officer()));

        assertTrue(savedLogs(2).stream().allMatch(e -> e.getDeliveryStatus() == DeliveryStatus.SIMULATED));
    }

    @Test
    void outsideWindowErrorsAreRecognised() {
        assertTrue(WhatsAppSender.isOutsideConversationWindow(new IOException("... \"code\":131047 ...")));
        assertFalse(WhatsAppSender.isOutsideConversationWindow(new IOException("401 invalid token")));
    }
}
