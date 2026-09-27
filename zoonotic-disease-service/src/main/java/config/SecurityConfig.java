package zoonotic_disease_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import zoonotic_disease_service.user.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // =========================
                        // SWAGGER
                        // =========================

                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // =========================
                        // APPROVAL WORKFLOW
                        // =========================

                        .requestMatchers(
                                "/api/zoonotic-incidents/*/approve",
                                "/api/zoonotic-incidents/*/reject",
                                "/api/zoonotic-incidents/*/request-correction"
                        ).hasRole(
                                "PROVINCIAL_SUPERVISOR"
                        )

                        // =========================
                        // AUDIT TRAIL API
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/audit-trails",
                                "/api/audit-trails/*"
                        ).hasAnyRole(
                                "PROVINCIAL_SUPERVISOR",
                                "PROVINCIAL_ADMIN",
                                "NATIONAL_USER"
                        )

                        // =========================
                        // CREATE INCIDENT
                        // =========================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/zoonotic-incidents"
                        ).hasAnyRole(
                                "WARD_RECORDER",
                                "PROVINCIAL_SUPERVISOR"
                        )

                        // =========================
                        // GET ALL INCIDENTS
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/zoonotic-incidents"
                        ).hasAnyRole(
                                "WARD_RECORDER",
                                "PROVINCIAL_SUPERVISOR",
                                "PROVINCIAL_ADMIN",
                                "NATIONAL_USER"
                        )

                        // =========================
                        // GET ONE INCIDENT
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/zoonotic-incidents/*"
                        ).hasAnyRole(
                                "WARD_RECORDER",
                                "PROVINCIAL_SUPERVISOR",
                                "PROVINCIAL_ADMIN",
                                "NATIONAL_USER"
                        )

                        // =========================
                        // UPDATE INCIDENT
                        // =========================

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/zoonotic-incidents/*"
                        ).hasAnyRole(
                                "WARD_RECORDER",
                                "PROVINCIAL_SUPERVISOR"
                        )

                        // =========================
                        // DELETE INCIDENT
                        // =========================

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/zoonotic-incidents/*"
                        ).hasAnyRole(
                                "WARD_RECORDER",
                                "PROVINCIAL_SUPERVISOR"
                        )

                        // =========================
                        // EVERYTHING ELSE
                        // =========================

                        .anyRequest().authenticated()
                )

                // =========================
                // JWT FILTER
                // =========================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // =========================
                // DISABLE BASIC AUTH
                // =========================

                .httpBasic(
                        AbstractHttpConfigurer::disable
                );

        return http.build();
    }
}