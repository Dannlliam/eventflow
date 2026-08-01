package com.eventflow.identity.interfaces;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * Simple authentication controller for development/demo purposes.
 * In production, this would integrate with an actual identity provider.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Value("${JWT_SECRET:eventflow-local-jwt-secret-key-min-256-bits}")
    private String jwtSecret;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // For development: hardcoded credentials
        // In production: validate against database/IdP
        if ("admin@eventflow.com".equals(request.email) && 
            "admin123".equals(request.password)) {
            
            String userId = "550e8400-e29b-41d4-a716-446655440000";
            String workspaceId = "660e8400-e29b-41d4-a716-446655440000";
            
            Date now = new Date();
            Date expiry = new Date(now.getTime() + 86400000); // 24 hours
            
            String token = Jwts.builder()
                .setSubject(userId)
                .claim("email", request.email)
                .claim("workspaceId", workspaceId)
                .claim("roles", "ADMIN,USER")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setIssuer("eventflow")
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
            
            return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of(
                    "id", userId,
                    "email", request.email,
                    "name", "Admin User",
                    "role", "ADMIN",
                    "workspaceId", workspaceId
                )
            ));
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    public record LoginRequest(String email, String password) {}
}
