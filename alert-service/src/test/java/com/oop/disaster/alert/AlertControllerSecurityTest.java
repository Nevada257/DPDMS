package com.oop.disaster.alert;

import com.oop.disaster.alert.controller.AlertController;
import com.oop.disaster.alert.repository.AlertLogRepository;
import com.oop.disaster.alert.repository.SubscriberRepository;
import com.oop.disaster.alert.security.JwtAuthenticationFilter;
import com.oop.disaster.alert.security.JwtService;
import com.oop.disaster.alert.security.SecurityConfig;
import com.oop.disaster.alert.service.AlertDispatcher;
import com.oop.disaster.alert.service.AlertRules;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API tests for hazard scoping and role rules on the alert endpoints. */
@WebMvcTest(AlertController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, AlertRules.class})
@TestPropertySource(properties = "jwt.secret=" + AlertControllerSecurityTest.SECRET)
class AlertControllerSecurityTest {

    static final String SECRET = "test-secret-key-for-dpdms-unit-tests-0123456789";

    @Autowired MockMvc mvc;
    @MockitoBean AlertDispatcher dispatcher;
    @MockitoBean AlertLogRepository logs;
    @MockitoBean SubscriberRepository subscribers;

    static String token(String user, String role, String scope) {
        return "Bearer " + Jwts.builder()
                .subject(user).claim("role", role).claim("hazardScope", scope)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static final String ACTIVE_FIRE = """
            {"hazard":"FIRE","incidentId":7,"ward":"Ward 1","district":"Rushinga",
             "severity":"HIGH","indicators":{"active":true}}""";

    @Test
    void fireRecorderCanRaiseFireAlert() throws Exception {
        mvc.perform(post("/api/alerts/incidents").header("Authorization", token("fire_recorder", "RECORDER", "FIRE"))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTIVE_FIRE))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.triggered").value(true));
        verify(dispatcher).dispatch(any(), anyString(), eq("fire_recorder"));
    }

    @Test
    void floodRecorderCannotRaiseFireAlert() throws Exception {
        mvc.perform(post("/api/alerts/incidents").header("Authorization", token("flood_recorder", "RECORDER", "FLOOD"))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTIVE_FIRE))
                .andExpect(status().isForbidden());
        verifyNoInteractions(dispatcher);
    }

    @Test
    void nationalUserCannotRaiseAlerts() throws Exception {
        mvc.perform(post("/api/alerts/incidents").header("Authorization", token("national_user", "NATIONAL", "ALL"))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTIVE_FIRE))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenWithoutHazardScopeIsRejected() throws Exception {
        mvc.perform(post("/api/alerts/incidents").header("Authorization", token("someone", "RECORDER", null))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTIVE_FIRE))
                .andExpect(status().isForbidden());
    }

    @Test
    void recorderWithScopeAllIsRejected() throws Exception {
        mvc.perform(post("/api/alerts/incidents").header("Authorization", token("sneaky", "RECORDER", "ALL"))
                        .contentType(MediaType.APPLICATION_JSON).content(ACTIVE_FIRE))
                .andExpect(status().isForbidden());
    }

    @Test
    void recorderCannotReadAlertLog() throws Exception {
        mvc.perform(get("/api/alerts/logs").header("Authorization", token("fire_recorder", "RECORDER", "FIRE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorSeesOnlyOwnHazardLog() throws Exception {
        mvc.perform(get("/api/alerts/logs?hazard=FLOOD")
                        .header("Authorization", token("fire_supervisor", "SUPERVISOR", "FIRE")))
                .andExpect(status().isOk());
        verify(logs).findByHazardIgnoreCaseOrderBySentAtDesc("FIRE");
    }

    @Test
    void onlyAdminManagesSubscribers() throws Exception {
        mvc.perform(get("/api/alerts/subscribers")
                        .header("Authorization", token("national_user", "NATIONAL", "ALL")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/alerts/subscribers")
                        .header("Authorization", token("provincial_admin", "ADMIN", "ALL")))
                .andExpect(status().isOk());
    }
}
