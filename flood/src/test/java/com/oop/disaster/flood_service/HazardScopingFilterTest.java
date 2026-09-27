package com.oop.disaster.flood_service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hazard-level scoping at the edge of the FLOOD service: only tokens issued
 * for FLOOD, or cross-hazard tokens held by NATIONAL / ADMIN users, get in.
 */
class HazardScopingFilterTest {

    static final String SECRET = "test-secret-key-for-dpdms-unit-tests-0123456789";

    private final JwtService jwtService = new JwtService(SECRET);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    static String token(String user, String role, String scope, String ward) {
        return Jwts.builder()
                .subject(user).claim("role", role).claim("hazardScope", scope).claim("ward", ward)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private MockHttpServletResponse run(String token, MockFilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/floods");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownHazardSupervisorIsAuthenticated() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        run(token("flood_supervisor", "SUPERVISOR", "FLOOD", null), chain);
        assertNotNull(chain.getRequest(), "request should continue down the chain");
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR")));
    }

    @Test
    void otherHazardSupervisorIsForbidden() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = run(token("x_supervisor", "SUPERVISOR", "DROUGHT", null), chain);
        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest(), "request must not reach the controller");
    }

    @Test
    void otherHazardRecorderIsForbidden() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = run(token("x_recorder", "RECORDER", "DROUGHT", "Ward 1"), chain);
        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void tokenWithoutHazardScopeIsForbidden() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = run(token("someone", "SUPERVISOR", null, null), chain);
        assertEquals(403, response.getStatus());
    }

    @Test
    void recorderCannotUseCrossHazardScope() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = run(token("sneaky", "RECORDER", "ALL", "Ward 1"), chain);
        assertEquals(403, response.getStatus());
    }

    @Test
    void nationalUserWithScopeAllIsAuthenticated() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        run(token("national_user", "NATIONAL", "ALL", null), chain);
        assertNotNull(chain.getRequest());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_NATIONAL")));
    }
}
