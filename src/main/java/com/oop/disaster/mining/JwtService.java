package com.oop.disaster.mining;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String SECRET =
            "DPDMS-SECRET-KEY-FOR-JWT-AUTHENTICATION-2026-SECURE";

    private final SecretKey key = Keys.hmacShaKeyFor(
            SECRET.getBytes(StandardCharsets.UTF_8)
    );

    public Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {

        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }

    public String extractHazardScope(String token) {

        return extractAllClaims(token)
                .get("hazardScope", String.class);
    }

    public boolean isValid(String token) {

        try {
            extractAllClaims(token);
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}