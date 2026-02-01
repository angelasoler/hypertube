package com.hypertube.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Search & Library Service
 * Manages video metadata, external API integration, and search functionality
 *
 * Features:
 * - Integration with YTS, EZTV, and TMDB APIs
 * - Video metadata storage and caching
 * - Search and filtering capabilities
 * - Popular videos aggregation
 * - User viewing history tracking
 * - Redis caching for performance
 * - Service discovery via Eureka
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableAsync
public class SearchLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchLibraryApplication.class, args);
    }
}
