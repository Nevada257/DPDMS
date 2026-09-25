package com.oop.disaster.flood_service;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder passwordEncoder) {

        UserDetails recorder = User.builder()
                .username("recorder")
                .password(passwordEncoder.encode("recorder123"))
                .roles("RECORDER")
                .build();

        UserDetails supervisor = User.builder()
                .username("supervisor")
                .password(passwordEncoder.encode("supervisor123"))
                .roles("SUPERVISOR")
                .build();

        UserDetails national = User.builder()
                .username("national")
                .password(passwordEncoder.encode("national123"))
                .roles("NATIONAL")
                .build();

        return new InMemoryUserDetailsManager(
                recorder,
                supervisor,
                national
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth

                        // Allow browser CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // Swagger/OpenAPI
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Supervisor-only approval workflow
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/floods/*/approve",
                                "/api/floods/*/reject",
                                "/api/floods/*/corrections"
                        ).hasRole("SUPERVISOR")

                        // Recorder can create flood incidents
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/floods"
                        ).hasRole("RECORDER")

                        // Recorder can update flood incidents
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/floods/*"
                        ).hasRole("RECORDER")

                        // Recorder can delete flood incidents
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/floods/*"
                        ).hasRole("RECORDER")

                        // Everyone with a valid role can READ
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/floods/**"
                        ).hasAnyRole(
                                "RECORDER",
                                "SUPERVISOR",
                                "NATIONAL"
                        )

                        // Anything else requires authentication
                        .anyRequest().authenticated()
                )

                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}