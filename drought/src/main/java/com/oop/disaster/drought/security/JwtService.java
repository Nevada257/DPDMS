package com.oop.disaster.drought.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    // ⚠️ This MUST match the secret used in your auth-service.
    //    It's loaded from an environment variable or application.properties.
    private final SecretKey signingKey;

    public JwtService(@org.springframework.beans.factory.annotation.Value("${jwt.secret:changeThisDefaultSecretKeyTo32CharsMin}") String secret) {
        // Secret must be at least 32 characters for HS256
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractHazardScope(String token) {
        return extractClaim(token, claims -> claims.get("hazardScope", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }

    public boolean isTokenValid(String token) {
        try {
            return !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public String generateToken(String username, String role, String hazardScope) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("hazardScope", hazardScope)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 8)) // 8 hours
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}