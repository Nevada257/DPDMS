package com.oop.disaster.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Reads approved incidents from one hazard service over REST, forwarding the
 * caller's JWT. Services are located through Eureka, with configured fallback URLs.
 */
@Component
public class HazardClient {

    /** Result of one call: the incidents, or the reason the service could not be read. */
    public record Result(Hazard hazard, List<IncidentView> incidents, String error) {
        public boolean ok() { return error == null; }
    }

    private final DiscoveryClient discovery;
    private final ObjectMapper mapper;
    private final Map<Hazard, String> fallbacks;
    private final Duration timeout;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public HazardClient(DiscoveryClient discovery, ObjectMapper mapper,
                        @Value("${dashboard.fallback.flood:http://localhost:8081}") String flood,
                        @Value("${dashboard.fallback.drought:http://localhost:8082}") String drought,
                        @Value("${dashboard.fallback.fire:http://localhost:8083}") String fire,
                        @Value("${dashboard.fallback.zoonotic:http://localhost:8084}") String zoonotic,
                        @Value("${dashboard.fallback.mining:http://localhost:8085}") String mining,
                        @Value("${dashboard.timeout-seconds:5}") long timeoutSeconds) {
        this.discovery = discovery;
        this.mapper = mapper;
        this.fallbacks = Map.of(Hazard.FLOOD, flood, Hazard.DROUGHT, drought, Hazard.FIRE, fire,
                Hazard.ZOONOTIC, zoonotic, Hazard.MINING, mining);
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public CompletableFuture<Result> fetchApproved(Hazard hazard, String authorizationHeader) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(baseUrl(hazard) + hazard.approvedPath()))
                    .timeout(timeout)
                    .header("Authorization", authorizationHeader)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
        } catch (Exception e) {
            return CompletableFuture.completedFuture(new Result(hazard, List.of(), e.getMessage()));
        }

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parse(hazard, response))
                .exceptionally(ex -> new Result(hazard, List.of(),
                        hazard.serviceId() + " unavailable: " + rootMessage(ex)));
    }

    Result parse(Hazard hazard, HttpResponse<String> response) {
        if (response.statusCode() == 403) {
            return new Result(hazard, List.of(), "forbidden for this user");
        }
        if (response.statusCode() / 100 != 2) {
            return new Result(hazard, List.of(), hazard.serviceId() + " returned HTTP " + response.statusCode());
        }
        try {
            return new Result(hazard, toApprovedViews(hazard, mapper.readTree(response.body())), null);
        } catch (Exception e) {
            return new Result(hazard, List.of(), "could not read " + hazard.serviceId() + " response");
        }
    }

    /** Normalises a JSON array and keeps APPROVED records only (pending records never reach the dashboard). */
    static List<IncidentView> toApprovedViews(Hazard hazard, JsonNode array) {
        List<IncidentView> out = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode node : array) {
                IncidentView view = IncidentNormalizer.normalize(hazard, node);
                if ("APPROVED".equals(view.approvalStatus())) {
                    out.add(view);
                }
            }
        }
        return out;
    }

    private String baseUrl(Hazard hazard) {
        try {
            List<ServiceInstance> instances = discovery.getInstances(hazard.serviceId());
            if (!instances.isEmpty()) {
                return instances.get(0).getUri().toString();
            }
        } catch (Exception ignored) {
            // registry unavailable - fall back to configured URL
        }
        return fallbacks.get(hazard);
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
}
