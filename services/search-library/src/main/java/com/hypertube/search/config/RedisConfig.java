package com.hypertube.search.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration with custom TTLs per cache
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Custom TTLs for different caches
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Video search results: 30 minutes
        cacheConfigurations.put("videoSearch", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Video details: 24 hours
        cacheConfigurations.put("videoDetails", defaultConfig.entryTtl(Duration.ofHours(24)));

        // Popular videos: 15 minutes
        cacheConfigurations.put("popularVideos", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // External API responses: 1 hour
        cacheConfigurations.put("externalApi", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
