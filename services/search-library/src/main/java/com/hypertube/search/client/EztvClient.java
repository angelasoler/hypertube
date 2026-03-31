package com.hypertube.search.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Client for EZTV API (TV show torrents)
 * API Documentation: https://eztv.re/api/get-torrents
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EztvClient {

    @Qualifier("eztvWebClient")
    private final WebClient eztvWebClient;

    /**
     * Get latest TV show torrents
     *
     * @param page Page number (1-indexed)
     * @param limit Number of results per page
     * @return EZTV API response
     */
    @Cacheable(value = "externalApi", key = "'eztv:latest:' + #page")
    public Mono<Map<String, Object>> getLatestTorrents(int page, int limit) {
        log.info("Fetching latest torrents from EZTV (page {})", page);

        return eztvWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/get-torrents")
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("Error fetching from EZTV: {}", error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Search TV shows on EZTV by IMDb ID
     *
     * @param imdbId IMDb ID
     * @return EZTV API response with torrents for the show
     */
    @Cacheable(value = "videoDetails", key = "'eztv:show:' + #imdbId")
    public Mono<Map<String, Object>> getShowByImdbId(String imdbId) {
        log.info("Fetching TV show from EZTV by IMDb ID: {}", imdbId);

        // Remove 'tt' prefix if present
        String numericId = imdbId.startsWith("tt") ? imdbId.substring(2) : imdbId;

        return eztvWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/get-torrents")
                .queryParam("imdb_id", numericId)
                .queryParam("limit", 100)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("Error fetching show from EZTV by IMDb ID {}: {}", imdbId, error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }
}
