package com.hypertube.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Utility for JWT token generation and validation
 * Uses HS512 algorithm with configurable secret and expiration
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationSeconds;
    private final String issuer;
    private final String audience;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration-seconds:3600}") long expirationSeconds,
        @Value("${jwt.issuer:hypertube}") String issuer,
        @Value("${jwt.audience:hypertube-api}") String audience
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
        this.audience = audience;
    }

    /**
     * Generates an access token for a user
     *
     * @param userId User's unique identifier
     * @param username User's username
     * @return JWT access token
     */
    public String generateAccessToken(UUID userId, String username) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("type", "access")
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact();
    }

    /**
     * Generates a refresh token for a user (longer lived)
     *
     * @param userId User's unique identifier
     * @return JWT refresh token
     */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationSeconds * 24); // 24x longer than access token

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact();
    }

    /**
     * Validates a JWT token and extracts claims
     *
     * @param token JWT token to validate
     * @return Claims if valid
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(issuer)
            .requireAudience(audience)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Extracts user ID from a valid token
     *
     * @param token JWT token
     * @return User ID
     */
    public UUID extractUserId(String token) {
        Claims claims = validateToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Checks if a token is expired
     *
     * @param token JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Gets the token type (access or refresh)
     *
     * @param token JWT token
     * @return Token type
     */
    public String getTokenType(String token) {
        Claims claims = validateToken(token);
        return claims.get("type", String.class);
    }
}
