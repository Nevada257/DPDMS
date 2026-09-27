package com.oop.disaster.drought;

import com.oop.disaster.drought.repository.DroughtIncidentRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API integration test: starts the DROUGHT service on a random port with its real
 * database and security, and exercises the approval workflow and the scoping rules
 * over HTTP with signed tokens, exactly as the gateway would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class DroughtApiIntegrationTest {

    @Value("${local.server.port}")
    int port;

    @Value("${jwt.secret}")
    String secret;

    @Autowired
    DroughtIncidentRepository repository;

    private final HttpClient http = HttpClient.newHttpClient();
    private final List<Long> created = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        created.forEach(id -> repository.findById(id).ifPresent(repository::delete));
    }

    private String token(String user, String role, String scope, String ward) {
        var builder = Jwts.builder()
                .setSubject(user)
                .claim("role", role)
                .claim("hazardScope", scope)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 300_000));
        if (ward != null) {
            builder.claim("ward", ward);
        }
        return builder.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private HttpResponse<String> call(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");
        request.method(method, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"?([^\",}]+)").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private String recorder() { return token("drought_recorder", "RECORDER", "DROUGHT", "Ward 1"); }
    private String supervisor() { return token("drought_supervisor", "SUPERVISOR", "DROUGHT", null); }
    private String national() { return token("national_user", "NATIONAL", "ALL", null); }
    private String otherHazardSupervisor() { return token("flood_supervisor", "SUPERVISOR", "FLOOD", null); }

    private long capture() throws Exception {
        HttpResponse<String> r = call("POST", "/api/drought/incidents", recorder(), BODY_WARD_1);
        assertEquals(200, r.statusCode(), r.body());
        long id = Long.parseLong(field(r.body(), "id"));
        created.add(id);
        return id;
    }

    static final String BODY_WARD_1 = """
            {"ward":"Ward 1","district":"Rushinga","province":"Mashonaland Central","dateTimeOfOccurrence":"2026-09-20T10:00:00","reporter":"x","severity":"HIGH","latitude":-16.62,"longitude":32.21,"rainfallDeficitMm":120.0,"consecutiveDryDays":40,"cropFailurePercentage":60.0,"peopleFacingWaterShortages":300,"livestockMortalityCount":5}""";

    @Test
    void recorderCapturesPendingIncidentVisibleOnlyToAuthorisedRoles() throws Exception {
        long id = capture();

        HttpResponse<String> own = call("GET", "/api/drought/incidents/" + id, recorder(), null);
        assertEquals(200, own.statusCode());
        assertEquals("PENDING", field(own.body(), "status"));
        assertEquals("drought_recorder", field(own.body(), "reporter"), "reporter comes from the token");

        assertEquals(200, call("GET", "/api/drought/incidents/" + id, supervisor(), null).statusCode());
        assertEquals(404, call("GET", "/api/drought/incidents/" + id, national(), null).statusCode(),
                "pending records are hidden from national users");
    }

    @Test
    void writesOutsideTheCallersScopeAreForbidden() throws Exception {
        assertEquals(403, call("POST", "/api/drought/incidents", national(), BODY_WARD_1).statusCode(),
                "national users are read-only");
        assertEquals(403, call("POST", "/api/drought/incidents", otherHazardSupervisor(), BODY_WARD_1).statusCode(),
                "another hazard's token is refused by this service");
        assertEquals(403, call("POST", "/api/drought/incidents", token("drought_recorder_w2", "RECORDER", "DROUGHT", "Ward 2"),
                BODY_WARD_1).statusCode(), "a recorder cannot capture for another ward");

        long id = capture();
        assertEquals(403, call("PATCH", "/api/drought/incidents/" + id + "/approve", recorder(), null).statusCode(),
                "recorders cannot approve");
        assertEquals(403, call("PATCH", "/api/drought/incidents/" + id + "/approve", national(), null).statusCode(),
                "national users cannot approve");
        assertEquals(403, call("GET", "/api/drought/incidents/" + id, otherHazardSupervisor(), null).statusCode(),
                "another hazard's supervisor cannot even read");
    }

    @Test
    void supervisorApprovalPublishesTheIncident() throws Exception {
        long id = capture();

        HttpResponse<String> approved = call("PATCH", "/api/drought/incidents/" + id + "/approve", supervisor(), null);
        assertEquals(200, approved.statusCode(), approved.body());
        assertEquals("APPROVED", field(approved.body(), "status"));

        HttpResponse<String> asNational = call("GET", "/api/drought/incidents/" + id, national(), null);
        assertEquals(200, asNational.statusCode(), "approved records are visible to national users");

        assertEquals(403, call("PUT", "/api/drought/incidents/" + id, recorder(), BODY_WARD_1).statusCode(),
                "approved records can no longer be edited");
    }
}
