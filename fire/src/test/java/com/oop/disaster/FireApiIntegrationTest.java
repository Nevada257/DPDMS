package com.oop.disaster;

import com.oop.disaster.repository.FireIncidentRepository;
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
 * API integration test: starts the FIRE service on a random port with its real
 * database and security, and exercises the approval workflow and the scoping rules
 * over HTTP with signed tokens, exactly as the gateway would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class FireApiIntegrationTest {

    @Value("${local.server.port}")
    int port;

    @Value("${jwt.secret}")
    String secret;

    @Autowired
    FireIncidentRepository repository;

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

    private String recorder() { return token("fire_recorder", "RECORDER", "FIRE", "Ward 1"); }
    private String supervisor() { return token("fire_supervisor", "SUPERVISOR", "FIRE", null); }
    private String national() { return token("national_user", "NATIONAL", "ALL", null); }
    private String otherHazardSupervisor() { return token("flood_supervisor", "SUPERVISOR", "FLOOD", null); }

    private long capture() throws Exception {
        HttpResponse<String> r = call("POST", "/api/fire-incidents", recorder(), BODY_WARD_1);
        assertEquals(200, r.statusCode(), r.body());
        long id = Long.parseLong(field(r.body(), "id"));
        created.add(id);
        return id;
    }

    static final String BODY_WARD_1 = """
            {"ward":"Ward 1","district":"Rushinga","province":"Mashonaland Central","occurrenceTime":"2026-09-20T10:00:00","severity":"HIGH","latitude":-16.62,"longitude":32.21,"areaBurned":12.5,"suspectedCause":"ACCIDENTAL","injuriesOrFatalities":1,"structuresDestroyed":2,"active":true}""";

    @Test
    void recorderCapturesPendingIncidentVisibleOnlyToAuthorisedRoles() throws Exception {
        long id = capture();

        HttpResponse<String> own = call("GET", "/api/fire-incidents/" + id, recorder(), null);
        assertEquals(200, own.statusCode());
        assertEquals("PENDING", field(own.body(), "status"));
        assertEquals("fire_recorder", field(own.body(), "reporter"), "reporter comes from the token");

        assertEquals(200, call("GET", "/api/fire-incidents/" + id, supervisor(), null).statusCode());
        assertEquals(404, call("GET", "/api/fire-incidents/" + id, national(), null).statusCode(),
                "pending records are hidden from national users");
    }

    @Test
    void writesOutsideTheCallersScopeAreForbidden() throws Exception {
        assertEquals(403, call("POST", "/api/fire-incidents", national(), BODY_WARD_1).statusCode(),
                "national users are read-only");
        assertEquals(403, call("POST", "/api/fire-incidents", otherHazardSupervisor(), BODY_WARD_1).statusCode(),
                "another hazard's token is refused by this service");
        assertEquals(403, call("POST", "/api/fire-incidents", token("fire_recorder_w2", "RECORDER", "FIRE", "Ward 2"),
                BODY_WARD_1).statusCode(), "a recorder cannot capture for another ward");

        long id = capture();
        assertEquals(403, call("PUT", "/api/fire-incidents/" + id + "/approve", recorder(), null).statusCode(),
                "recorders cannot approve");
        assertEquals(403, call("PUT", "/api/fire-incidents/" + id + "/approve", national(), null).statusCode(),
                "national users cannot approve");
        assertEquals(403, call("GET", "/api/fire-incidents/" + id, otherHazardSupervisor(), null).statusCode(),
                "another hazard's supervisor cannot even read");
    }

    @Test
    void supervisorApprovalPublishesTheIncident() throws Exception {
        long id = capture();

        HttpResponse<String> approved = call("PUT", "/api/fire-incidents/" + id + "/approve", supervisor(), null);
        assertEquals(200, approved.statusCode(), approved.body());
        assertEquals("APPROVED", field(approved.body(), "status"));

        HttpResponse<String> asNational = call("GET", "/api/fire-incidents/" + id, national(), null);
        assertEquals(200, asNational.statusCode(), "approved records are visible to national users");

        assertEquals(403, call("PUT", "/api/fire-incidents/" + id, recorder(), BODY_WARD_1).statusCode(),
                "approved records can no longer be edited");
    }
}
