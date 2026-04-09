package com.hypertube.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filterFactory;
    private String secret = "mySecretKeyMySecretKeyMySecretKeyMySecretKey"; // Must be long enough
    private String issuer = "hypertube";
    private String audience = "hypertube-api";

    @BeforeEach
    public void setUp() {
        filterFactory = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filterFactory, "jwtSecret", secret);
        ReflectionTestUtils.setField(filterFactory, "jwtIssuer", issuer);
        ReflectionTestUtils.setField(filterFactory, "jwtAudience", audience);
    }

    private String generateToken() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject("12345")
                .claim("email", "user@example.com")
                .claim("username", "user123")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    private Jwt createJwt() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", issuer);
        claims.put("aud", Collections.singletonList(audience));
        claims.put("sub", "12345");
        claims.put("email", "user@example.com");
        claims.put("username", "user123");

        return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Collections.singletonMap("alg", "HS256"), claims);
    }

    @Test
    public void verifyHeadersAdded_ManualPath() {
        String token = generateToken();
        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationFilter.Config());

        GatewayFilterChain chain = (exchange) -> {
            ServerHttpRequest req = exchange.getRequest();
            assertEquals("12345", req.getHeaders().getFirst("X-User-Id"));
            assertEquals("user@example.com", req.getHeaders().getFirst("X-User-Email"));
            assertEquals("user123", req.getHeaders().getFirst("X-User-Username"));
            return Mono.empty();
        };

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();
    }

    @Test
    public void verifyHeadersAdded_OptimizedPath() {
        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationFilter.Config());

        GatewayFilterChain chain = (exchange) -> {
            ServerHttpRequest req = exchange.getRequest();
            assertEquals("12345", req.getHeaders().getFirst("X-User-Id"));
            assertEquals("user@example.com", req.getHeaders().getFirst("X-User-Email"));
            assertEquals("user123", req.getHeaders().getFirst("X-User-Username"));
            return Mono.empty();
        };

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                // Authorization header is not strictly needed if context is present,
                // but filter might check it? No, optimized path does not check header.
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Jwt jwt = createJwt();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        filter.filter(exchange, chain)
              .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
              .block();
    }
}
