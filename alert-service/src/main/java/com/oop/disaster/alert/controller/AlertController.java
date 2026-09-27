package com.oop.disaster.alert.controller;

import com.oop.disaster.alert.model.AlertLog;
import com.oop.disaster.alert.model.Subscriber;
import com.oop.disaster.alert.repository.AlertLogRepository;
import com.oop.disaster.alert.repository.SubscriberRepository;
import com.oop.disaster.alert.security.AuthUser;
import com.oop.disaster.alert.service.AlertDispatcher;
import com.oop.disaster.alert.service.AlertRules;
import com.oop.disaster.alert.service.IncidentAlertRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRules rules;
    private final AlertDispatcher dispatcher;
    private final AlertLogRepository logs;
    private final SubscriberRepository subscribers;

    public AlertController(AlertRules rules, AlertDispatcher dispatcher,
                           AlertLogRepository logs, SubscriberRepository subscribers) {
        this.rules = rules;
        this.dispatcher = dispatcher;
        this.logs = logs;
        this.subscribers = subscribers;
    }

    /**
     * Called by a hazard service (with the acting user's token) when an
     * incident is captured, edited or approved. If the incident meets the
     * alerting criteria the alert is queued and 202 Accepted is returned
     * immediately; delivery happens in the background.
     */
    @PostMapping("/incidents")
    public ResponseEntity<Map<String, Object>> incident(@Valid @RequestBody IncidentAlertRequest request,
                                                        @AuthenticationPrincipal AuthUser user) {
        if (!user.coversHazard(request.hazard())) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Forbidden: token is not valid for hazard " + request.hazard()));
        }

        Optional<String> reason = rules.evaluate(request);
        if (reason.isEmpty()) {
            return ResponseEntity.ok(Map.of("triggered", false));
        }

        dispatcher.dispatch(request, reason.get(), user.username());
        return ResponseEntity.accepted().body(Map.of("triggered", true, "reason", reason.get()));
    }

    /** Alert log. Supervisors see their own hazard only; admin and national users see all. */
    @GetMapping("/logs")
    public List<AlertLog> logs(@RequestParam(required = false) String hazard,
                               @AuthenticationPrincipal AuthUser user) {
        if (!"ALL".equalsIgnoreCase(user.hazardScope())) {
            return logs.findByHazardIgnoreCaseOrderBySentAtDesc(user.hazardScope());
        }
        return hazard == null || hazard.isBlank()
                ? logs.findAllByOrderBySentAtDesc()
                : logs.findByHazardIgnoreCaseOrderBySentAtDesc(hazard);
    }

    /**
     * Sends a test alert to every active subscriber by email and WhatsApp, so the
     * provincial administrator can confirm delivery. Results appear in the alert log.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test(@AuthenticationPrincipal AuthUser user) {
        dispatcher.dispatchTest(user.username());
        return ResponseEntity.accepted().body(Map.of(
                "queued", true,
                "subscribers", subscribers.findAll().stream().filter(Subscriber::isActive).count()));
    }

    // ---------------- Subscribers (provincial administrator only) ----------------

    @GetMapping("/subscribers")
    public List<Subscriber> subscribers() {
        return subscribers.findAll();
    }

    @PostMapping("/subscribers")
    public Subscriber addSubscriber(@Valid @RequestBody Subscriber subscriber) {
        return subscribers.save(subscriber);
    }

    @DeleteMapping("/subscribers/{id}")
    public ResponseEntity<Void> removeSubscriber(@PathVariable Long id) {
        if (!subscribers.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        subscribers.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
