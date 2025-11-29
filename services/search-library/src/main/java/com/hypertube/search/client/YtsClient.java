package com.hypertube.search.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Client for YTS API (movie torrents)
 * API Documentation: https://yts.mx/api
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YtsClient {

    @Qualifier("ytsWebClient")
    private final WebClient ytsWebClient;

    /**
     * Search movies on YTS
     *
     * @param query Search query
     * @param page Page number (1-indexed)
     * @return YTS API response
     */
    @Cacheable(value = "externalApi", key = "'yts:search:' + #query + ':' + #page")
    public Mono<Map<String, Object>> searchMovies(String query, int page) {
        log.info("Searching YTS for query: {} (page {})", query, page);

        return ytsWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/list_movies.json")
                .queryParam("query_term", query)
                .queryParam("page", page)
                .queryParam("limit", 20)
                .queryParam("sort_by", "seeds")
                .queryParam("order_by", "desc")
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("Error fetching from YTS: {}", error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Get popular movies from YTS
     *
     * @param page Page number (1-indexed)
     * @return YTS API response with popular movies
     */
    @Cacheable(value = "popularVideos", key = "'yts:popular:' + #page")
    public Mono<Map<String, Object>> getPopularMovies(int page) {
        log.info("Fetching popular movies from YTS (page {})", page);

        return ytsWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/list_movies.json")
                .queryParam("page", page)
                .queryParam("limit", 20)
                .queryParam("sort_by", "seeds")
                .queryParam("order_by", "desc")
                .queryParam("minimum_rating", 6)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("Error fetching popular movies from YTS: {}", error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Get movie details by IMDb ID
     *
     * @param imdbId IMDb ID
     * @return YTS API response with movie details
     */
    @Cacheable(value = "videoDetails", key = "'yts:movie:' + #imdbId")
    public Mono<Map<String, Object>> getMovieByImdbId(String imdbId) {
        log.info("Fetching movie from YTS by IMDb ID: {}", imdbId);

        return ytsWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/list_movies.json")
                .queryParam("query_term", imdbId)
                .queryParam("limit", 1)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("Error fetching movie from YTS by IMDb ID {}: {}", imdbId, error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }
}
