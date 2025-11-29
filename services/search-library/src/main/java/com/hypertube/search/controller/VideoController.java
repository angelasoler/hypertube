package com.hypertube.search.controller;

import com.hypertube.search.dto.PagedResponse;
import com.hypertube.search.dto.SearchRequest;
import com.hypertube.search.dto.VideoDTO;
import com.hypertube.search.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for video search and browsing
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;

    /**
     * Search videos with filters
     * GET /api/videos/search?query=inception&genre=Action&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<VideoDTO>> searchVideos(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) String genre,
        @RequestParam(required = false) Integer minYear,
        @RequestParam(required = false) Integer maxYear,
        @RequestParam(required = false) java.math.BigDecimal minRating,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection,
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = userIdHeader != null ? UUID.fromString(userIdHeader) : null;

        SearchRequest request = SearchRequest.builder()
            .query(query)
            .genre(genre)
            .minYear(minYear)
            .maxYear(maxYear)
            .minRating(minRating)
            .page(page)
            .size(size)
            .sortBy(sortBy)
            .sortDirection(sortDirection)
            .build();

        PagedResponse<VideoDTO> response = videoService.searchVideos(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get popular videos
     * GET /api/videos/popular?page=0&size=20
     */
    @GetMapping("/popular")
    public ResponseEntity<PagedResponse<VideoDTO>> getPopularVideos(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = userIdHeader != null ? UUID.fromString(userIdHeader) : null;
        PagedResponse<VideoDTO> response = videoService.getPopularVideos(page, size, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get video details by ID
     * GET /api/videos/{videoId}
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<VideoDTO> getVideoById(
        @PathVariable UUID videoId,
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = userIdHeader != null ? UUID.fromString(userIdHeader) : null;
        VideoDTO video = videoService.getVideoById(videoId, userId);
        return ResponseEntity.ok(video);
    }

    /**
     * Get video details by IMDb ID
     * GET /api/videos/imdb/{imdbId}
     */
    @GetMapping("/imdb/{imdbId}")
    public ResponseEntity<VideoDTO> getVideoByImdbId(
        @PathVariable String imdbId,
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = userIdHeader != null ? UUID.fromString(userIdHeader) : null;
        VideoDTO video = videoService.getVideoByImdbId(imdbId, userId);
        return ResponseEntity.ok(video);
    }

    /**
     * Mark video as watched
     * POST /api/videos/{videoId}/watch
     */
    @PostMapping("/{videoId}/watch")
    public ResponseEntity<Map<String, String>> markAsWatched(
        @PathVariable UUID videoId,
        @RequestHeader("X-User-Id") String userIdHeader
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        videoService.markAsWatched(videoId, userId);
        return ResponseEntity.ok(Map.of("message", "Video marked as watched"));
    }

    /**
     * Get user's watched videos
     * GET /api/videos/watched?page=0&size=20
     */
    @GetMapping("/watched")
    public ResponseEntity<PagedResponse<VideoDTO>> getWatchedVideos(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestHeader("X-User-Id") String userIdHeader
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        PagedResponse<VideoDTO> response = videoService.getWatchedVideos(userId, page, size);
        return ResponseEntity.ok(response);
    }
}
