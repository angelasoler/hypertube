package com.hypertube.streaming.controller;

import com.hypertube.streaming.dto.DownloadJobDTO;
import com.hypertube.streaming.dto.DownloadMessage;
import com.hypertube.streaming.entity.DownloadJob;
import com.hypertube.streaming.entity.Subtitle;
import com.hypertube.streaming.repository.DownloadJobRepository;
import com.hypertube.streaming.repository.VideoTorrentRepository;
import com.hypertube.streaming.service.CacheManagementService;
import com.hypertube.streaming.service.SubtitleService;
import com.hypertube.streaming.service.TorrentService;
import com.hypertube.streaming.service.VideoStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/streaming")
@RequiredArgsConstructor
@Slf4j
public class StreamingController {

    private final DownloadJobRepository downloadJobRepository;
    private final VideoTorrentRepository videoTorrentRepository;
    private final TorrentService torrentService;
    private final VideoStreamingService videoStreamingService;
    private final SubtitleService subtitleService;
    private final CacheManagementService cacheManagementService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queues.download}")
    private String downloadQueue;

    @GetMapping("/jobs")
    public ResponseEntity<List<DownloadJobDTO>> getAllJobs() {
        List<DownloadJob> jobs = downloadJobRepository.findAll();
        List<DownloadJobDTO> jobDTOs = jobs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobDTOs);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<DownloadJobDTO> getJob(@PathVariable UUID jobId) {
        return downloadJobRepository.findById(jobId)
                .map(this::mapToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs/user/{userId}")
    public ResponseEntity<List<DownloadJobDTO>> getUserJobs(@PathVariable UUID userId) {
        List<DownloadJob> jobs = downloadJobRepository.findByUserId(userId);
        List<DownloadJobDTO> jobDTOs = jobs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobDTOs);
    }

    /**
     * Initiates a video download job.
     */
    @PostMapping("/download")
    public ResponseEntity<DownloadJobDTO> initiateDownload(@RequestBody DownloadMessage request) {
        try {
            log.info("Initiating download for video: {}, torrent: {}",
                    request.getVideoId(), request.getTorrentId());

            // Create download job
            DownloadJob job = new DownloadJob();
            job.setVideoId(request.getVideoId());
            job.setTorrentId(request.getTorrentId());
            job.setUserId(request.getUserId());
            job.setStatus(DownloadJob.DownloadStatus.PENDING);
            job.setProgress(0);
            job.setCreatedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());

            job = downloadJobRepository.save(job);

            // Send message to download queue
            DownloadMessage message = DownloadMessage.builder()
                    .jobId(job.getId())
                    .videoId(request.getVideoId())
                    .torrentId(request.getTorrentId())
                    .userId(request.getUserId())
                    .magnetLink(request.getMagnetLink())
                    .torrentUrl(request.getTorrentUrl())
                    .build();

            rabbitTemplate.convertAndSend(downloadQueue, message);

            log.info("Download job created: {}", job.getId());

            return ResponseEntity.ok(mapToDTO(job));

        } catch (Exception e) {
            log.error("Error initiating download", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Checks if a download job is ready for streaming (buffer threshold reached).
     */
    @GetMapping("/jobs/{jobId}/ready")
    public ResponseEntity<Map<String, Object>> checkStreamingReadiness(@PathVariable UUID jobId) {
        try {
            DownloadJob job = downloadJobRepository.findById(jobId)
                    .orElse(null);

            if (job == null) {
                return ResponseEntity.notFound().build();
            }

            boolean isReady = torrentService.isReadyForStreaming(jobId);
            String filePath = torrentService.getFilePath(jobId);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("ready", isReady);
            response.put("status", job.getStatus());
            response.put("progress", job.getProgress());
            response.put("filePath", filePath);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking streaming readiness for job: {}", jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cancels a download job.
     */
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<Void> cancelDownload(@PathVariable UUID jobId) {
        try {
            torrentService.cancelDownload(jobId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error cancelling download for job: {}", jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Streams a video with HTTP Range request support (RFC 7233).
     *
     * Supports:
     * - Full file streaming (no Range header)
     * - Partial content streaming (Range header present)
     * - Video seeking during active downloads
     * - Progressive playback in browsers
     *
     * @param jobId The download job ID
     * @param rangeHeader Optional Range header for partial content requests
     * @return Video content with appropriate status code (200 or 206)
     */
    @GetMapping("/video/{jobId}")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable UUID jobId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        log.info("Streaming video for job: {} (Range: {})", jobId, rangeHeader != null ? rangeHeader : "none");

        return videoStreamingService.streamVideo(jobId, rangeHeader);
    }

    /**
     * Gets all subtitles for a video.
     *
     * @param videoId The video ID
     * @return List of available subtitles with their languages
     */
    @GetMapping("/subtitles/{videoId}")
    public ResponseEntity<List<Map<String, Object>>> getSubtitles(@PathVariable UUID videoId) {
        try {
            List<Subtitle> subtitles = subtitleService.getSubtitlesForVideo(videoId);

            List<Map<String, Object>> response = subtitles.stream()
                    .map(subtitle -> {
                        Map<String, Object> subtitleInfo = new HashMap<>();
                        subtitleInfo.put("id", subtitle.getId());
                        subtitleInfo.put("languageCode", subtitle.getLanguageCode());
                        subtitleInfo.put("format", subtitle.getFormat());
                        subtitleInfo.put("available", subtitleService.isValidSubtitleFile(subtitle.getFilePath()));
                        return subtitleInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting subtitles for video: {}", videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Streams a subtitle file for a video.
     *
     * @param videoId The video ID
     * @param languageCode The language code (e.g., "en", "pt", "es")
     * @return The subtitle file in WebVTT format
     */
    @GetMapping("/subtitles/{videoId}/{languageCode}")
    public ResponseEntity<Resource> getSubtitleFile(
            @PathVariable UUID videoId,
            @PathVariable String languageCode) {

        try {
            Subtitle subtitle = subtitleService.getSubtitleByLanguage(videoId, languageCode);

            if (subtitle == null) {
                log.warn("Subtitle not found for video: {}, language: {}", videoId, languageCode);
                return ResponseEntity.notFound().build();
            }

            File subtitleFile = new File(subtitle.getFilePath());
            if (!subtitleFile.exists()) {
                log.error("Subtitle file not found: {}", subtitle.getFilePath());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(subtitleFile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/vtt"));
            headers.setContentLength(subtitleFile.length());
            headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

            log.info("Serving subtitle for video: {}, language: {}", videoId, languageCode);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error serving subtitle for video: {}, language: {}", videoId, languageCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets cache statistics (storage usage, cached videos count, etc.).
     *
     * @return Cache statistics including total storage, used space, and video count
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> stats = cacheManagementService.getCacheStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Manually triggers cache cleanup to remove expired videos.
     *
     * @return Number of videos cleaned up
     */
    @PostMapping("/cache/cleanup")
    public ResponseEntity<Map<String, Object>> manualCleanup() {
        try {
            int cleanedCount = cacheManagementService.manualCleanup();

            Map<String, Object> response = new HashMap<>();
            response.put("cleanedCount", cleanedCount);
            response.put("message", "Cleanup completed successfully");

            log.info("Manual cleanup completed: {} videos removed", cleanedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during manual cleanup", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Clears all cached videos (admin function).
     * WARNING: This will remove all cached videos from the system.
     *
     * @return Number of videos cleared
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            int clearedCount = cacheManagementService.clearAllCache();

            Map<String, Object> response = new HashMap<>();
            response.put("clearedCount", clearedCount);
            response.put("message", "Cache cleared successfully");

            log.warn("Cache cleared: {} videos removed", clearedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Streaming service is running");
    }

    private DownloadJobDTO mapToDTO(DownloadJob job) {
        return DownloadJobDTO.builder()
                .id(job.getId())
                .videoId(job.getVideoId())
                .torrentId(job.getTorrentId())
                .userId(job.getUserId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .downloadSpeed(job.getDownloadSpeed())
                .etaSeconds(job.getEtaSeconds())
                .filePath(job.getFilePath())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
