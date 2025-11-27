package com.hypertube.search.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Client for TMDB API (The Movie Database)
 * Used for fetching detailed movie metadata, cast, and images
 * API Documentation: https://developers.themoviedb.org/3
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TmdbClient {

    @Qualifier("tmdbWebClient")
    private final WebClient tmdbWebClient;

    @Value("${external-apis.tmdb.api-key}")
    private String apiKey;

    /**
     * Search movies on TMDB
     *
     * @param query Search query
     * @param page Page number (1-indexed)
     * @return TMDB API response
     */
    @Cacheable(value = "externalApi", key = "'tmdb:search:' + #query + ':' + #page")
    public Mono<Map<String, Object>> searchMovies(String query, int page) {
        log.info("Searching TMDB for query: {} (page {})", query, page);

        return tmdbWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/search/movie")
                .queryParam("api_key", apiKey)
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("include_adult", false)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("Error searching TMDB: {}", error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Get movie details by IMDb ID
     *
     * @param imdbId IMDb ID
     * @return TMDB API response with movie details
     */
    @Cacheable(value = "videoDetails", key = "'tmdb:movie:' + #imdbId")
    public Mono<Map<String, Object>> getMovieByImdbId(String imdbId) {
        log.info("Fetching movie from TMDB by IMDb ID: {}", imdbId);

        return tmdbWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/find/" + imdbId)
                .queryParam("api_key", apiKey)
                .queryParam("external_source", "imdb_id")
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("Error fetching movie from TMDB by IMDb ID {}: {}", imdbId, error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Get movie credits (cast and crew)
     *
     * @param tmdbId TMDB movie ID
     * @return TMDB API response with credits
     */
    @Cacheable(value = "videoDetails", key = "'tmdb:credits:' + #tmdbId")
    public Mono<Map<String, Object>> getMovieCredits(Long tmdbId) {
        log.info("Fetching movie credits from TMDB for movie ID: {}", tmdbId);

        return tmdbWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/movie/" + tmdbId + "/credits")
                .queryParam("api_key", apiKey)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("Error fetching credits from TMDB for movie {}: {}", tmdbId, error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Get movie details by TMDB ID
     *
     * @param tmdbId TMDB movie ID
     * @return TMDB API response with detailed movie info
     */
    @Cacheable(value = "videoDetails", key = "'tmdb:details:' + #tmdbId")
    public Mono<Map<String, Object>> getMovieDetails(Long tmdbId) {
        log.info("Fetching movie details from TMDB for movie ID: {}", tmdbId);

        return tmdbWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/movie/" + tmdbId)
                .queryParam("api_key", apiKey)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("Error fetching details from TMDB for movie {}: {}", tmdbId, error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }
}
