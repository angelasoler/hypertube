package com.hypertube.streaming.service;

import com.hypertube.streaming.config.StreamingConfig;
import com.hypertube.streaming.entity.CachedVideo;
import com.hypertube.streaming.repository.CachedVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing video cache lifecycle.
 *
 * Features:
 * - Automatic cleanup of expired videos (1-month TTL)
 * - LRU eviction when storage is full
 * - Cache statistics and monitoring
 * - Disk space management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheManagementService {

    private final CachedVideoRepository cachedVideoRepository;
    private final SubtitleService subtitleService;
    private final StreamingConfig streamingConfig;

    private static final long ONE_MONTH_DAYS = 30;
    private static final long CLEANUP_INTERVAL_MS = 3600000; // 1 hour
    private static final double STORAGE_THRESHOLD_PERCENT = 90.0; // Trigger cleanup at 90% full

    /**
     * Scheduled task to clean up expired cached videos.
     * Runs every hour.
     */
    @Scheduled(fixedDelay = CLEANUP_INTERVAL_MS)
    @Transactional
    public void cleanupExpiredVideos() {
        log.info("Starting scheduled cleanup of expired cached videos");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<CachedVideo> expiredVideos = cachedVideoRepository.findByExpiresAtBefore(now);

            if (expiredVideos.isEmpty()) {
                log.info("No expired videos found");
                return;
            }

            log.info("Found {} expired videos to clean up", expiredVideos.size());

            int successCount = 0;
            int failCount = 0;

            for (CachedVideo video : expiredVideos) {
                try {
                    deleteVideo(video);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to delete expired video: {}", video.getId(), e);
                    failCount++;
                }
            }

            log.info("Expired video cleanup completed: {} deleted, {} failed", successCount, failCount);

        } catch (Exception e) {
            log.error("Error during scheduled cleanup", e);
        }
    }

    /**
     * Checks disk usage and triggers LRU eviction if needed.
     * Runs every 30 minutes.
     */
    @Scheduled(fixedDelay = 1800000) // 30 minutes
    @Transactional
    public void checkStorageAndEvict() {
        log.info("Checking storage usage");

        try {
            File storageDir = new File(streamingConfig.getStorage().getBasePath());
            if (!storageDir.exists()) {
                log.warn("Storage directory does not exist: {}", storageDir.getPath());
                return;
            }

            long totalSpace = storageDir.getTotalSpace();
            long freeSpace = storageDir.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            double usagePercent = (usedSpace * 100.0) / totalSpace;

            log.info("Storage usage: {:.2f}% ({} MB used / {} MB total)",
                    usagePercent,
                    usedSpace / (1024 * 1024),
                    totalSpace / (1024 * 1024));

            if (usagePercent >= STORAGE_THRESHOLD_PERCENT) {
                log.warn("Storage usage is at {:.2f}%, triggering LRU eviction", usagePercent);
                evictLeastRecentlyUsed();
            }

        } catch (Exception e) {
            log.error("Error checking storage usage", e);
        }
    }

    /**
     * Evicts the least recently used cached videos to free up space.
     * Removes videos until usage drops below threshold.
     */
    @Transactional
    public void evictLeastRecentlyUsed() {
        log.info("Starting LRU eviction");

        try {
            File storageDir = new File(streamingConfig.getStorage().getBasePath());
            long totalSpace = storageDir.getTotalSpace();
            long targetUsage = (long) (totalSpace * (STORAGE_THRESHOLD_PERCENT - 10.0) / 100.0);

            // Get cached videos ordered by last access time (LRU)
            List<CachedVideo> videos = cachedVideoRepository.findAll();
            videos.sort((a, b) -> a.getLastAccessedAt() != null && b.getLastAccessedAt() != null
                    ? a.getLastAccessedAt().compareTo(b.getLastAccessedAt())
                    : (a.getCachedAt().compareTo(b.getCachedAt())));

            int evictedCount = 0;

            for (CachedVideo video : videos) {
                long currentUsed = totalSpace - storageDir.getFreeSpace();

                if (currentUsed < targetUsage) {
                    log.info("Storage usage is now acceptable, stopping eviction");
                    break;
                }

                try {
                    log.info("Evicting LRU video: {} (last accessed: {})",
                            video.getId(),
                            video.getLastAccessedAt() != null ? video.getLastAccessedAt() : "never");

                    deleteVideo(video);
                    evictedCount++;

                } catch (Exception e) {
                    log.error("Failed to evict video: {}", video.getId(), e);
                }
            }

            log.info("LRU eviction completed: {} videos removed", evictedCount);

        } catch (Exception e) {
            log.error("Error during LRU eviction", e);
        }
    }

    /**
     * Manually deletes a cached video and its associated files.
     *
     * @param video The cached video to delete
     */
    @Transactional
    public void deleteVideo(CachedVideo video) {
        try {
            // Delete video file
            File videoFile = new File(video.getFilePath());
            if (videoFile.exists()) {
                boolean deleted = videoFile.delete();
                if (deleted) {
                    log.info("Deleted video file: {}", video.getFilePath());
                } else {
                    log.warn("Failed to delete video file: {}", video.getFilePath());
                }
            } else {
                log.warn("Video file not found: {}", video.getFilePath());
            }

            // Delete associated subtitles
            try {
                subtitleService.deleteSubtitlesForVideo(video.getVideoId());
            } catch (Exception e) {
                log.error("Error deleting subtitles for video: {}", video.getVideoId(), e);
            }

            // Delete database entry
            cachedVideoRepository.delete(video);
            log.info("Deleted cached video entry: {}", video.getId());

        } catch (Exception e) {
            log.error("Error deleting video: {}", video.getId(), e);
            throw new RuntimeException("Failed to delete video", e);
        }
    }

    /**
     * Gets cache statistics.
     *
     * @return Map containing cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Count total cached videos
            long totalVideos = cachedVideoRepository.count();
            stats.put("totalVideos", totalVideos);

            // Count expired videos
            LocalDateTime now = LocalDateTime.now();
            List<CachedVideo> expiredVideos = cachedVideoRepository.findByExpiresAtBefore(now);
            stats.put("expiredVideos", expiredVideos.size());

            // Calculate total cache size
            List<CachedVideo> allVideos = cachedVideoRepository.findAll();
            long totalSize = allVideos.stream()
                    .mapToLong(CachedVideo::getFileSize)
                    .sum();
            stats.put("totalSizeBytes", totalSize);
            stats.put("totalSizeMB", totalSize / (1024 * 1024));

            // Get storage info
            File storageDir = new File(streamingConfig.getStorage().getBasePath());
            if (storageDir.exists()) {
                long totalSpace = storageDir.getTotalSpace();
                long freeSpace = storageDir.getFreeSpace();
                long usedSpace = totalSpace - freeSpace;
                double usagePercent = (usedSpace * 100.0) / totalSpace;

                stats.put("storageTotal MB", totalSpace / (1024 * 1024));
                stats.put("storageFreeBytes", freeSpace);
                stats.put("storageFreeMB", freeSpace / (1024 * 1024));
                stats.put("storageUsedBytes", usedSpace);
                stats.put("storageUsedMB", usedSpace / (1024 * 1024));
                stats.put("storageUsagePercent", String.format("%.2f", usagePercent));
            }

            log.info("Cache statistics: {} videos, {} MB total",
                    totalVideos, totalSize / (1024 * 1024));

        } catch (Exception e) {
            log.error("Error calculating cache statistics", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * Manually triggers cleanup of expired videos.
     *
     * @return Number of videos deleted
     */
    @Transactional
    public int manualCleanup() {
        log.info("Manual cleanup triggered");

        LocalDateTime now = LocalDateTime.now();
        List<CachedVideo> expiredVideos = cachedVideoRepository.findByExpiresAtBefore(now);

        int deleteCount = 0;
        for (CachedVideo video : expiredVideos) {
            try {
                deleteVideo(video);
                deleteCount++;
            } catch (Exception e) {
                log.error("Failed to delete video: {}", video.getId(), e);
            }
        }

        log.info("Manual cleanup completed: {} videos deleted", deleteCount);
        return deleteCount;
    }

    /**
     * Updates the last accessed time for a cached video.
     *
     * @param cachedVideoId The cached video ID
     */
    @Transactional
    public void updateLastAccessed(java.util.UUID cachedVideoId) {
        cachedVideoRepository.findById(cachedVideoId).ifPresent(video -> {
            video.setLastAccessedAt(LocalDateTime.now());
            cachedVideoRepository.save(video);
            log.debug("Updated last accessed time for cached video: {}", cachedVideoId);
        });
    }

    /**
     * Clears all cached videos (use with caution).
     *
     * @return Number of videos deleted
     */
    @Transactional
    public int clearAllCache() {
        log.warn("Clearing all cached videos");

        List<CachedVideo> allVideos = cachedVideoRepository.findAll();
        int deleteCount = 0;

        for (CachedVideo video : allVideos) {
            try {
                deleteVideo(video);
                deleteCount++;
            } catch (Exception e) {
                log.error("Failed to delete video: {}", video.getId(), e);
            }
        }

        log.warn("All cache cleared: {} videos deleted", deleteCount);
        return deleteCount;
    }
}
