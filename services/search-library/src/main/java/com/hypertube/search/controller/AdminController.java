package com.hypertube.search.controller;

import com.hypertube.search.service.VideoImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for managing video library
 */
@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final VideoImportService videoImportService;

    /**
     * Import popular movies from YTS
     * POST /api/admin/videos/import/yts?pages=5
     */
    @PostMapping("/import/yts")
    public ResponseEntity<Map<String, Object>> importFromYts(
        @RequestParam(defaultValue = "5") int pages
    ) {
        log.info("Starting YTS import for {} pages", pages);

        try {
            int imported = videoImportService.importPopularMoviesFromYts(pages);

            return ResponseEntity.ok(Map.of(
                "message", "Import completed successfully",
                "imported", imported,
                "source", "YTS"
            ));
        } catch (Exception e) {
            log.error("Failed to import from YTS", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Import failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Search and import specific movies
     * POST /api/admin/videos/import/search?query=inception
     */
    @PostMapping("/import/search")
    public ResponseEntity<Map<String, Object>> importBySearch(
        @RequestParam String query
    ) {
        log.info("Searching and importing videos for: {}", query);

        try {
            var imported = videoImportService.searchAndImport(query);

            return ResponseEntity.ok(Map.of(
                "message", "Search and import completed",
                "imported", imported.size(),
                "query", query
            ));
        } catch (Exception e) {
            log.error("Failed to import from search", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Import failed: " + e.getMessage()
            ));
        }
    }
}
