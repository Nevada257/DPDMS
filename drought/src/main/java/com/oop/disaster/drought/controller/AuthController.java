package com.oop.disaster.drought.controller;

import com.oop.disaster.drought.security.JwtService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Temporary login endpoint for testing.
     * Provide username + role + hazardScope and get a JWT back.
     *
     * Example:
     *   POST /auth/login
     *   {
     *     "username": "recorder1",
     *     "role": "DROUGHT_RECORDER",
     *     "hazardScope": "DROUGHT"
     *   }
     */
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "anonymous");
        String role = body.getOrDefault("role", "NATIONAL_USER");
        String hazardScope = body.getOrDefault("hazardScope", "DROUGHT");

        String token = jwtService.generateToken(username, role, hazardScope);
        return Map.of(
                "token", token,
                "username", username,
                "role", role,
                "hazardScope", hazardScope
        );
    }
}