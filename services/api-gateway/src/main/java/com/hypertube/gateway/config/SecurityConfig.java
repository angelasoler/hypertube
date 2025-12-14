package com.hypertube.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for API Gateway
 *
 * Handles:
 * - JWT authentication
 * - CORS configuration
 * - Public vs protected endpoints
 * - CSRF protection (disabled for stateless JWT API - see CSRF_PROTECTION.md)
 *
 * CSRF Protection Decision:
 * CSRF is DISABLED because this is a stateless API using JWT tokens in Authorization headers.
 * CSRF attacks exploit automatic cookie inclusion by browsers. Since we:
 * 1. Do NOT use cookies for authentication
 * 2. Use JWT in Authorization headers (requires explicit JavaScript)
 * 3. Implement CORS to restrict cross-origin requests
 * 4. Validate JWT issuer and audience
 *
 * CSRF protection would provide no security benefit in this architecture.
 * See /CSRF_PROTECTION.md for detailed analysis and review criteria.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // Create a SecretKey from the JWT secret string
        // Use HmacSHA512 to match the user-service JWT generation (HS512)
        byte[] secretKeyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, "HmacSHA512");

        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // CSRF disabled - stateless API with JWT in headers (not cookies)
            // See CSRF_PROTECTION.md for detailed justification
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints - no authentication required
                // Standard API paths (non-versioned, non-service-prefixed)
                .pathMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .pathMatchers("/api/auth/oauth/**").permitAll()
                .pathMatchers("/api/auth/password-reset/**").permitAll()

                // V1 API paths (versioned)
                .pathMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                .pathMatchers("/api/v1/auth/oauth/**").permitAll()
                .pathMatchers("/api/v1/auth/password-reset/**").permitAll()

                // Frontend service-based paths
                .pathMatchers("/api/users/auth/login", "/api/users/auth/register").permitAll()
                .pathMatchers("/api/users/auth/oauth/**").permitAll()
                .pathMatchers("/api/users/auth/password-reset/**").permitAll()
                .pathMatchers("/api/users/auth/logout").permitAll()

                // Actuator endpoints
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()

                // Streaming endpoints - temporary public access for testing
                .pathMatchers("/api/streaming/**", "/api/v1/streaming/**").permitAll()

                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            )
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow frontend origins
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:8080",
            "https://hypertube.com" // Production frontend URL
        ));

        configuration.setAllowedMethods(Arrays.asList(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
