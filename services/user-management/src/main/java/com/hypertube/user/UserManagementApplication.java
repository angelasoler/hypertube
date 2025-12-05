package com.hypertube.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * User Management Service
 * Handles user registration, authentication, profile management, and OAuth2 integration
 *
 * Features:
 * - Standard registration with username, email, password
 * - OAuth2 integration (42 school, Google, GitHub)
 * - JWT-based authentication
 * - Password reset via email
 * - Profile management (email, profile picture, language preferences)
 * - Bcrypt password hashing (cost factor 12)
 * - Service discovery via Eureka
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class UserManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }
}
