package zoonotic_disease_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Notifies the shared alert-service about an incident. The alert-service
 * applies the alerting criteria and sends email / WhatsApp alerts.
 *
 * The call is fire-and-forget (asynchronous HTTP): incident capture never
 * waits for it, and if the alert-service is down the failure is only logged,
 * so this service keeps working (graceful degradation). The alert-service is
 * located through Eureka; ALERT_SERVICE_URL is used if it is not registered.
 * The caller's own JWT is forwarded, so hazard scoping applies there too.
 */
@Component
public class AlertClient {

    private static final Logger log = LoggerFactory.getLogger(AlertClient.class);

    private final DiscoveryClient discovery;
    private final String fallbackUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public AlertClient(DiscoveryClient discovery,
                       @Value("${alert.service.url:http://localhost:8088}") String fallbackUrl) {
        this.discovery = discovery;
        this.fallbackUrl = fallbackUrl;
    }

    public void notifyIncident(String hazard, Long incidentId, String ward, String district, String province,
                               Object severity, Double latitude, Double longitude,
                               Map<String, Object> indicators) {
        try {
            String auth = currentAuthorizationHeader();
            if (auth == null) {
                return; // alerts are only raised on behalf of an authenticated user
            }

            StringBuilder json = new StringBuilder("{");
            field(json, "hazard", hazard).append(',');
            field(json, "incidentId", incidentId).append(',');
            field(json, "ward", ward).append(',');
            field(json, "district", district).append(',');
            field(json, "province", province).append(',');
            field(json, "severity", severity).append(',');
            field(json, "latitude", latitude).append(',');
            field(json, "longitude", longitude).append(',');
            json.append("\"indicators\":{");
            boolean first = true;
            for (Map.Entry<String, Object> e : indicators.entrySet()) {
                if (!first) {
                    json.append(',');
                }
                field(json, e.getKey(), e.getValue());
                first = false;
            }
            json.append("}}");

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/alerts/incidents"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("Authorization", auth)
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> {
                        if (r.statusCode() / 100 != 2) {
                            log.warn("alert-service answered {} for {} incident {}: {}",
                                    r.statusCode(), hazard, incidentId, r.body());
                        }
                    })
                    .exceptionally(ex -> {
                        log.warn("alert-service unavailable, alert for {} incident {} not sent: {}",
                                hazard, incidentId, ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            // Never let alerting break incident capture
            log.warn("Could not queue alert for {} incident {}: {}", hazard, incidentId, e.getMessage());
        }
    }

    private String baseUrl() {
        try {
            List<ServiceInstance> instances = discovery.getInstances("alert-service");
            if (!instances.isEmpty()) {
                return instances.get(0).getUri().toString();
            }
        } catch (Exception ignored) {
            // registry unavailable - fall back to the configured URL
        }
        return fallbackUrl;
    }

    private static String currentAuthorizationHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String header = attrs.getRequest().getHeader("Authorization");
            return header != null && header.startsWith("Bearer ") ? header : null;
        }
        return null;
    }

    private static StringBuilder field(StringBuilder sb, String name, Object value) {
        sb.append('"').append(escape(name)).append("\":");
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else {
            sb.append('"').append(escape(value.toString())).append('"');
        }
        return sb;
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
