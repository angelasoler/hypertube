package com.hypertube.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Authentication Filter
 *
 * Validates JWT tokens with comprehensive security checks:
 * - Signature verification
 * - Expiration validation
 * - Issuer/Audience validation
 * - Claim sanitization before forwarding
 *
 * All validation failures result in 401 Unauthorized responses.
 */
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.issuer:hypertube}")
    private String jwtIssuer;

    @Value("${jwt.audience:hypertube-api}")
    private String jwtAudience;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip JWT validation for public endpoints
            if (isPublicEndpoint(request.getPath().toString())) {
                return chain.filter(exchange);
            }

            // Extract JWT token from Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // Validate and parse JWT with comprehensive security checks
                Claims claims = validateToken(token);

                // Sanitize and validate claims before forwarding to downstream services
                String userId = sanitizeClaimValue(claims.getSubject());
                String userEmail = sanitizeClaimValue(claims.get("email", String.class));
                String username = sanitizeClaimValue(claims.get("username", String.class));

                // Validate required claims are present
                if (!StringUtils.hasText(userId)) {
                    log.warn("JWT token missing required 'sub' claim");
                    return onError(exchange, "Invalid token: missing user ID", HttpStatus.UNAUTHORIZED);
                }

                // Add user information to request headers for downstream services
                ServerHttpRequest.Builder requestBuilder = request.mutate()
                    .header("X-User-Id", userId);

                if (StringUtils.hasText(userEmail)) {
                    requestBuilder.header("X-User-Email", userEmail);
                }
                if (StringUtils.hasText(username)) {
                    requestBuilder.header("X-User-Username", username);
                }

                ServerHttpRequest modifiedRequest = requestBuilder.build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (ExpiredJwtException e) {
                log.warn("JWT token expired: {}", e.getMessage());
                return onError(exchange, "Token expired", HttpStatus.UNAUTHORIZED);
            } catch (SignatureException e) {
                log.error("Invalid JWT signature: {}", e.getMessage());
                return onError(exchange, "Invalid token signature", HttpStatus.UNAUTHORIZED);
            } catch (MalformedJwtException e) {
                log.error("Malformed JWT token: {}", e.getMessage());
                return onError(exchange, "Malformed token", HttpStatus.UNAUTHORIZED);
            } catch (UnsupportedJwtException e) {
                log.error("Unsupported JWT token: {}", e.getMessage());
                return onError(exchange, "Unsupported token", HttpStatus.UNAUTHORIZED);
            } catch (IllegalArgumentException e) {
                log.error("JWT claims string is empty: {}", e.getMessage());
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            } catch (JwtException e) {
                log.error("JWT validation failed: {}", e.getMessage());
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.error("Unexpected error during JWT validation: {}", e.getMessage(), e);
                return onError(exchange, "Authentication failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * Validates JWT token with comprehensive security checks:
     * - Signature verification
     * - Expiration validation
     * - Not-before validation
     * - Issuer validation
     * - Audience validation
     *
     * @param token JWT token string
     * @return Validated claims
     * @throws JwtException if validation fails
     */
    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(jwtIssuer)  // Validate issuer
            .requireAudience(jwtAudience)  // Validate audience
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Sanitizes claim values to prevent injection attacks.
     * Removes potentially dangerous characters and limits length.
     *
     * @param value Claim value to sanitize
     * @return Sanitized value or null
     */
    private String sanitizeClaimValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        // Remove control characters and limit length
        String sanitized = value
            .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "") // Remove control chars except whitespace
            .trim();

        // Prevent header injection attacks - remove newlines and carriage returns
        sanitized = sanitized.replaceAll("[\r\n]", "");

        // Limit length to prevent DOS attacks
        if (sanitized.length() > 255) {
            log.warn("Claim value exceeds maximum length, truncating");
            sanitized = sanitized.substring(0, 255);
        }

        return sanitized.isEmpty() ? null : sanitized;
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/auth/login") ||
               path.contains("/auth/register") ||
               path.contains("/auth/oauth") ||
               path.contains("/auth/password-reset") ||
               path.contains("/actuator/health");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties if needed
    }
}
