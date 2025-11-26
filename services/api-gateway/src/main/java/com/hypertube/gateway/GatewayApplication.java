package com.hypertube.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Application for HyperTube
 *
 * This gateway handles:
 * - Request routing to microservices
 * - JWT authentication and authorization
 * - Rate limiting
 * - CORS configuration
 * - Circuit breaking
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
