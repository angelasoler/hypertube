package com.hypertube.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Validates JWT configuration at application startup.
 *
 * CRITICAL SECURITY: Ensures JWT secret is properly configured before
 * the application starts accepting requests.
 */
@Component
public class JwtConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtConfigValidator.class);

    private static final int MINIMUM_SECRET_LENGTH = 32; // 256 bits
    private static final List<String> FORBIDDEN_SECRETS = Arrays.asList(
        "your-256-bit-secret-key-change-this-in-production",
        "change_this_to_a_secure_256_bit_secret_key",
        "secret",
        "password",
        "changeme"
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")
    private long jwtExpiration;

    @Value("${jwt.issuer:hypertube}")
    private String jwtIssuer;

    /**
     * Validates JWT configuration at startup.
     * Application will fail to start if configuration is invalid.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtConfig() {
        log.info("Validating JWT configuration...");

        // Check if JWT secret is set
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "SECURITY ERROR: JWT_SECRET environment variable is not set. " +
                "Application cannot start without a valid JWT secret."
            );
        }

        // Check minimum length (256 bits = 32 bytes)
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException(
                String.format(
                    "SECURITY ERROR: JWT_SECRET is too short (%d bytes). " +
                    "Minimum required: %d bytes (256 bits). " +
                    "Use a cryptographically secure random string.",
                    secretBytes.length, MINIMUM_SECRET_LENGTH
                )
            );
        }

        // Check for forbidden/default values
        String secretLowerCase = jwtSecret.toLowerCase();
        for (String forbidden : FORBIDDEN_SECRETS) {
            if (secretLowerCase.contains(forbidden)) {
                throw new IllegalStateException(
                    "SECURITY ERROR: JWT_SECRET contains a forbidden default value. " +
                    "Please set JWT_SECRET to a cryptographically secure random string."
                );
            }
        }

        // Validate expiration time
        if (jwtExpiration <= 0) {
            throw new IllegalStateException(
                "SECURITY ERROR: JWT expiration must be greater than 0 seconds."
            );
        }

        // Warn if expiration is too long
        if (jwtExpiration > 86400) { // More than 24 hours
            log.warn(
                "WARNING: JWT expiration is set to {} seconds ({} hours). " +
                "Consider using shorter expiration times with refresh tokens for better security.",
                jwtExpiration, jwtExpiration / 3600
            );
        }

        log.info("JWT configuration validated successfully");
        log.info("JWT Issuer: {}", jwtIssuer);
        log.info("JWT Expiration: {} seconds", jwtExpiration);
        log.info("JWT Secret Length: {} bytes", secretBytes.length);
    }
}
