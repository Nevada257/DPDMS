package com.dpdms.auth_service;

import java.util.Map;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        if (userRepository.findByUsername(user.getUsername()) != null) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Username already exists"));
        }

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