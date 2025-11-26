package com.hypertube.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Rate Limiting Configuration
 *
 * Implements tiered rate limiting strategy:
 * - Stricter limits for authentication endpoints (prevent brute force)
 * - Standard limits for general API endpoints
 * - Per-IP address tracking using Redis
 *
 * Rate limits are defined in application.yml per route.
 */
@Configuration
public class RateLimitConfig {

    /**
     * IP-based rate limiting key resolver.
     * Uses client IP address to track rate limits.
     *
     * In production behind a proxy/load balancer, ensure X-Forwarded-For
     * header is properly configured and trusted.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // Try to get real IP from X-Forwarded-For header (for proxy scenarios)
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                // Take first IP in chain (original client)
                String clientIp = forwardedFor.split(",")[0].trim();
                return Mono.just(clientIp);
            }

            // Fallback to remote address
            String remoteAddr = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

            return Mono.just(remoteAddr);
        };
    }

    /**
     * User-based rate limiting (for authenticated requests).
     * Uses JWT subject (user ID) to track rate limits per user.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Extract user ID from X-User-Id header (added by JwtAuthenticationFilter)
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

            // Fall back to IP-based limiting if user not authenticated
            if (userId == null || userId.isEmpty()) {
                String remoteAddr = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
                return Mono.just("ip:" + remoteAddr);
            }

            return Mono.just("user:" + userId);
        };
    }
}
