package com.hypertube.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

public class RateLimitConfigTest {

    private final RateLimitConfig rateLimitConfig = new RateLimitConfig();
    private final KeyResolver ipKeyResolver = rateLimitConfig.ipKeyResolver();

    @Test
    void testResolveIpFromHeaderSimple() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "192.168.1.1")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(ipKeyResolver.resolve(exchange))
                .expectNext("192.168.1.1")
                .verifyComplete();
    }

    @Test
    void testResolveIpFromHeaderMulti() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(ipKeyResolver.resolve(exchange))
                .expectNext("192.168.1.1")
                .verifyComplete();
    }

    @Test
    void testResolveIpFromHeaderWithWhitespace() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "  192.168.1.1  , 10.0.0.1")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(ipKeyResolver.resolve(exchange))
                .expectNext("192.168.1.1")
                .verifyComplete();
    }

    @Test
    void testResolveIpFromHeaderTrailingComma() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "192.168.1.1,")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(ipKeyResolver.resolve(exchange))
                .expectNext("192.168.1.1")
                .verifyComplete();
    }

    @Test
    void testFallbackToRemoteAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(ipKeyResolver.resolve(exchange))
                .expectNext("127.0.0.1")
                .verifyComplete();
    }
}
