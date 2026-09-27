package com.oop.disaster.alert.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying a valid Bearer token. The principal is an
 * {@link AuthUser}. Tokens without a hazardScope claim, or with scope ALL on a
 * role other than ADMIN / NATIONAL, are rejected with 403.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        AuthUser user;
        try {
            user = jwtService.parse(header.substring(7));
        } catch (Exception invalidToken) {
            chain.doFilter(request, response); // unauthenticated -> 401/403 from Spring Security
            return;
        }

        boolean scopeOk = user.hazardScope() != null
                && (!"ALL".equalsIgnoreCase(user.hazardScope())
                    || user.hasRole("ADMIN") || user.hasRole("NATIONAL"));
        if (!scopeOk || user.role() == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden: token has no valid hazard scope\"}");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().toUpperCase())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }
}
