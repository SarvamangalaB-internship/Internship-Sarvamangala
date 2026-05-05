package com.urbanvogue.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    // ── Generate token when user logs in ──
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)        // ← Store role in token
                .setIssuedAt(new Date())
                .setExpiration(new Date(
                        new Date().getTime() + jwtExpirationMs
                ))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Extract username from token ──
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // ── Extract role from token ──
    public String getRoleFromToken(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
    }

    // ── Validate token is genuine and not expired ──
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token);
            return true;    // Token is valid ✅
        } catch (ExpiredJwtException e) {
            System.out.println("Token expired: " + e.getMessage());
            return false;   // Token expired ❌
        } catch (Exception e) {
            System.out.println("Invalid token: " + e.getMessage());
            return false;   // Token tampered ❌
        }
    }

    // ── Check if token is expired ──
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}