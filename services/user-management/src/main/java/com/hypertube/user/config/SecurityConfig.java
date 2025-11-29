package com.hypertube.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for User Management Service
 *
 * Handles:
 * - Public endpoints for standard auth (register, login, password reset)
 * - OAuth2 login configuration for 42 school, Google, GitHub
 * - Protected profile endpoints
 *
 * Note: API Gateway handles JWT validation and rate limiting for requests
 * This service configures OAuth2 flows that redirect to provider login pages
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Stateless JWT API
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/auth/**",
                    "/api/oauth2/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()
                // Protected endpoints (authentication handled by API Gateway)
                .requestMatchers("/api/users/**").authenticated()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
