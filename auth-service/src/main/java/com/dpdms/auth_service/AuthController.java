package com.dpdms.auth_service;

import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    private static final Set<String> ROLES = Set.of("RECORDER", "SUPERVISOR", "ADMIN", "NATIONAL");
    private static final Set<String> HAZARDS = Set.of("FLOOD", "DROUGHT", "FIRE", "ZOONOTIC", "MINING");

    /**
     * Creates a user account. Only a provincial administrator (ADMIN token)
     * may register users, so nobody can self-assign a role.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")
                || !jwtService.isValid(authHeader.substring(7))
                || !"ADMIN".equals(jwtService.extractRole(authHeader.substring(7)))) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Only a provincial administrator can register users"));
        }

        if (user.getUsername() == null || user.getUsername().isBlank()
                || user.getPassword() == null || user.getPassword().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username and a password of at least 8 characters are required"));
        }

        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        String scope = user.getHazardScope() == null ? "" : user.getHazardScope().toUpperCase();

        if (!ROLES.contains(role)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unknown role: " + user.getRole()));
        }
        // Recorders and supervisors are scoped to exactly one hazard;
        // admin and national users span all hazards.
        if ((role.equals("RECORDER") || role.equals("SUPERVISOR")) && !HAZARDS.contains(scope)) {
            return ResponseEntity.badRequest().body(Map.of("message", "A single hazardScope is required for " + role));
        }
        if (role.equals("RECORDER") && (user.getWard() == null || user.getWard().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("message", "A ward is required for RECORDER"));
        }
        if (role.equals("ADMIN") || role.equals("NATIONAL")) {
            scope = "ALL";
        }

        if (userRepository.findByUsername(user.getUsername()) != null) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Username already exists"));
        }

        user.setRole(role);
        user.setHazardScope(scope);
        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of("message", "User registered successfully")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> loginRequest) {

        String username = loginRequest.get("username");

        if (username == null || loginRequest.get("password") == null) {
            return ResponseEntity
                    .status(401)
                    .body(Map.of("message", "Invalid username or password"));
        }
        String password = loginRequest.get("password");

        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ResponseEntity
                    .status(401)
                    .body(Map.of("message", "Invalid username or password"));
        }

        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            return ResponseEntity
                    .status(401)
                    .body(Map.of("message", "Invalid username or password"));
        }

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(
                Map.of(
                        "token", token,
                        "username", user.getUsername(),
                        "role", user.getRole(),
                        "hazardScope", user.getHazardScope() == null ? "" : user.getHazardScope(),
                        "ward", user.getWard() == null ? "" : user.getWard()
                )
        );
    }
}