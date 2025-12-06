package com.hypertube.streaming.service;

import com.hypertube.streaming.config.StreamingConfig;
import com.hypertube.streaming.entity.DownloadJob;
import com.hypertube.streaming.repository.CachedVideoRepository;
import com.hypertube.streaming.repository.DownloadJobRepository;
import com.hypertube.streaming.worker.DownloadWorker;
import com.hypertube.torrent.TorrentMetadata;
import com.hypertube.torrent.bencode.BencodeException;
import com.hypertube.torrent.manager.DownloadException;
import com.hypertube.torrent.manager.DownloadManager;
import com.hypertube.torrent.manager.PieceSelectionStrategy;
import com.hypertube.torrent.tracker.TrackerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * TorrentService - Handles BitTorrent downloads for video streaming using custom torrent library.
 *
 * Features:
 * - Sequential piece downloading (optimized for streaming)
 * - Multi-peer concurrent downloads
 * - Progress tracking and callbacks
 * - Buffer threshold for early streaming
 * - Automatic file verification with SHA-1 hashes
 */
@Service
@Slf4j
public class TorrentService {

    private static final double BUFFER_THRESHOLD_PERCENT = 10.0; // 10% buffer before streaming

    private final StreamingConfig streamingConfig;
    private final DownloadJobRepository downloadJobRepository;
    private final CachedVideoRepository cachedVideoRepository;
    private final DownloadWorker downloadWorker;

    private TrackerClient trackerClient;
    private final Map<UUID, DownloadManager> activeDownloads = new ConcurrentHashMap<>();
    private final Map<UUID, Path> downloadPaths = new ConcurrentHashMap<>();
    private final ScheduledExecutorService progressMonitor = Executors.newScheduledThreadPool(1);
    private final ExecutorService downloadExecutor = Executors.newCachedThreadPool();

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(UUID jobId, int progress, long downloadSpeed, int etaSeconds);
    }

    public TorrentService(StreamingConfig streamingConfig,
                         DownloadJobRepository downloadJobRepository,
                         CachedVideoRepository cachedVideoRepository,
                         @Lazy DownloadWorker downloadWorker) {
        this.streamingConfig = streamingConfig;
        this.downloadJobRepository = downloadJobRepository;
        this.cachedVideoRepository = cachedVideoRepository;
        this.downloadWorker = downloadWorker;
    }

    @PostConstruct
    public void init() {
        log.info("=================================================================");
        log.info("Initializing TorrentService with custom BitTorrent library");
        log.info("=================================================================");

        try {
            // Create download directory
            Path downloadPath = Paths.get(streamingConfig.getTorrent().getDownloadPath());
            Files.createDirectories(downloadPath);

            // Initialize tracker client
            int port = streamingConfig.getTorrent().getPortRangeStart();
            trackerClient = new TrackerClient(port);

            log.info("Service configuration:");
            log.info("- Download path: {}", streamingConfig.getTorrent().getDownloadPath());
            log.info("- Listening port: {}", port);
            log.info("- Max connections: {}", streamingConfig.getTorrent().getMaxConnections());
            log.info("- Buffer threshold: {}%", BUFFER_THRESHOLD_PERCENT);
            log.info("- DHT enabled: {}", streamingConfig.getTorrent().getDhtEnabled());

            log.info("TorrentService initialized successfully");

        } catch (IOException e) {
            log.error("Failed to initialize TorrentService", e);
            throw new RuntimeException("Failed to initialize TorrentService", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TorrentService");

        // Stop all active downloads
        activeDownloads.values().forEach(manager -> {
            try {
                manager.stop();
            } catch (Exception e) {
                log.warn("Error stopping download manager", e);
            }
        });

        activeDownloads.clear();
        downloadPaths.clear();

        // Shutdown executors
        progressMonitor.shutdown();
        downloadExecutor.shutdown();

        try {
            if (!progressMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                progressMonitor.shutdownNow();
            }
            if (!downloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            progressMonitor.shutdownNow();
            downloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("TorrentService shutdown complete");
    }

    /**
     * Starts a torrent download with sequential piece downloading.
     *
     * @param jobId The download job ID
     * @param videoId The video ID
     * @param torrentId The torrent ID
     * @param magnetOrUrl The magnet link or torrent URL
     * @param progressCallback Callback for progress updates
     */
    public void startDownload(UUID jobId, UUID videoId, UUID torrentId,
                            String magnetOrUrl, ProgressCallback progressCallback) {
        log.info("Starting torrent download for job: {}", jobId);
        log.info("Video ID: {}, Torrent ID: {}", videoId, torrentId);
        log.info("Magnet/URL: {}", magnetOrUrl);

        downloadExecutor.submit(() -> {
            try {
                // Parse torrent metadata
                TorrentMetadata metadata = parseTorrentMetadata(magnetOrUrl);
                log.info("Parsed torrent: {} ({} bytes, {} pieces)",
                    metadata.getName(), metadata.getTotalSize(), metadata.getNumPieces());

                // Generate download file path
                Path downloadPath = generateDownloadPath(videoId, metadata.getName());
                downloadPaths.put(jobId, downloadPath);

                // Create download manager with SEQUENTIAL strategy (best for streaming)
                DownloadManager manager = new DownloadManager(
                    metadata,
                    trackerClient,
                    downloadPath,
                    PieceSelectionStrategy.SEQUENTIAL
                );

                activeDownloads.put(jobId, manager);

                // Update job status
                updateJobStatus(jobId, DownloadJob.DownloadStatus.DOWNLOADING);

                // Start download
                manager.start();
                log.info("Download started for job: {}", jobId);

                // Start progress monitoring
                scheduleProgressMonitoring(jobId, manager, progressCallback);

                // Wait for completion in background
                waitForCompletion(jobId, manager, downloadPath);

            } catch (BencodeException e) {
                log.error("Failed to parse torrent metadata for job: {}", jobId, e);
                markJobFailed(jobId, "Invalid torrent format: " + e.getMessage());

            } catch (DownloadException e) {
                log.error("Failed to start download for job: {}", jobId, e);
                markJobFailed(jobId, "Download failed: " + e.getMessage());

            } catch (IOException e) {
                log.error("I/O error during download for job: {}", jobId, e);
                markJobFailed(jobId, "I/O error: " + e.getMessage());

            } catch (Exception e) {
                log.error("Unexpected error during download for job: {}", jobId, e);
                markJobFailed(jobId, "Unexpected error: " + e.getMessage());
            }
        });
    }

    /**
     * Checks if a download has reached the buffer threshold and is ready for streaming.
     *
     * @param jobId The download job ID
     * @return true if ready for streaming, false otherwise
     */
    public boolean isReadyForStreaming(UUID jobId) {
        DownloadManager manager = activeDownloads.get(jobId);
        if (manager == null) {
            return false;
        }

        double progress = manager.getProgress();
        return progress >= BUFFER_THRESHOLD_PERCENT;
    }

    /**
     * Gets the file path for a download job.
     *
     * @param jobId The download job ID
     * @return The file path, or null if not available
     */
    public String getFilePath(UUID jobId) {
        Path path = downloadPaths.get(jobId);
        if (path != null && Files.exists(path)) {
            return path.toString();
        }
        return null;
    }

    /**
     * Cancels a download job.
     *
     * @param jobId The download job ID
     */
    public void cancelDownload(UUID jobId) {
        log.info("Cancelling download for job: {}", jobId);

        DownloadManager manager = activeDownloads.remove(jobId);
        if (manager != null) {
            try {
                manager.stop();
                log.info("Download stopped for job: {}", jobId);
            } catch (Exception e) {
                log.warn("Error stopping download manager for job: {}", jobId, e);
            }
        }

        downloadPaths.remove(jobId);

        updateJobStatus(jobId, DownloadJob.DownloadStatus.CANCELLED);
    }

    /**
     * Parse torrent metadata from magnet link or .torrent file
     */
    private TorrentMetadata parseTorrentMetadata(String magnetOrUrl) throws BencodeException, IOException {
        if (magnetOrUrl.startsWith("magnet:")) {
            return TorrentMetadata.fromMagnetLink(magnetOrUrl);
        } else {
            // Assume it's a URL to a .torrent file
            // TODO: Download .torrent file from URL
            throw new BencodeException("Direct .torrent URL not yet supported, use magnet links");
        }
    }

    /**
     * Generate download file path based on video ID and torrent name
     */
    private Path generateDownloadPath(UUID videoId, String torrentName) throws IOException {
        Path baseDir = Paths.get(streamingConfig.getTorrent().getDownloadPath());
        Files.createDirectories(baseDir);

        // Use video ID as directory name to avoid conflicts
        Path videoDir = baseDir.resolve(videoId.toString());
        Files.createDirectories(videoDir);

        // Use torrent name as filename (sanitize it)
        String sanitizedName = torrentName.replaceAll("[^a-zA-Z0-9.-]", "_");
        return videoDir.resolve(sanitizedName);
    }

    /**
     * Schedule periodic progress monitoring
     */
    private void scheduleProgressMonitoring(UUID jobId, DownloadManager manager, ProgressCallback callback) {
        progressMonitor.scheduleAtFixedRate(() -> {
            try {
                if (!activeDownloads.containsKey(jobId)) {
                    return; // Download was cancelled or completed
                }

                double progress = manager.getProgress();
                long downloadSpeed = manager.getDownloadSpeed();
                long downloaded = manager.getDownloaded().get();
                long remaining = 0; // TODO: Calculate remaining bytes
                int etaSeconds = downloadSpeed > 0 ? (int) (remaining / downloadSpeed) : -1;

                // Call progress callback
                if (callback != null) {
                    callback.onProgress(jobId, (int) progress, downloadSpeed, etaSeconds);
                }

                log.debug("Job {}: Progress={:.2f}%, Speed={} KB/s, Peers={}",
                    jobId, progress, downloadSpeed / 1024, manager.getActivePeerCount());

            } catch (Exception e) {
                log.warn("Error monitoring progress for job: {}", jobId, e);
            }
        }, 1, 2, TimeUnit.SECONDS);
    }

    /**
     * Wait for download completion in background thread
     */
    private void waitForCompletion(UUID jobId, DownloadManager manager, Path downloadPath) {
        try {
            // Poll until complete
            while (!manager.isComplete() && activeDownloads.containsKey(jobId)) {
                Thread.sleep(1000);
            }

            if (manager.isComplete()) {
                log.info("Download completed for job: {}", jobId);

                // Stop the download manager
                manager.stop();
                activeDownloads.remove(jobId);

                // Notify completion
                markJobCompleted(jobId, downloadPath.toString());

            } else {
                log.info("Download was cancelled for job: {}", jobId);
            }

        } catch (InterruptedException e) {
            log.warn("Download monitoring interrupted for job: {}", jobId);
            Thread.currentThread().interrupt();

        } catch (Exception e) {
            log.error("Error waiting for download completion for job: {}", jobId, e);
            markJobFailed(jobId, "Error during download: " + e.getMessage());
        }
    }

    /**
     * Update job status in database
     */
    private void updateJobStatus(UUID jobId, DownloadJob.DownloadStatus status) {
        try {
            downloadJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(status);
                job.setUpdatedAt(LocalDateTime.now());
                downloadJobRepository.save(job);
            });
        } catch (Exception e) {
            log.error("Error updating job status for job: {}", jobId, e);
        }
    }

    /**
     * Mark job as completed and trigger conversion if needed
     */
    private void markJobCompleted(UUID jobId, String filePath) {
        try {
            // Delegate to DownloadWorker which handles conversion workflow
            downloadWorker.markJobCompleted(jobId, filePath);
            log.info("Download job marked as completed: {}", jobId);
        } catch (Exception e) {
            log.error("Error marking job as completed: {}", jobId, e);
        }
    }

    /**
     * Mark job as failed
     */
    private void markJobFailed(UUID jobId, String errorMessage) {
        try {
            DownloadManager manager = activeDownloads.remove(jobId);
            if (manager != null) {
                manager.stop();
            }

            downloadPaths.remove(jobId);

            // Delegate to DownloadWorker
            downloadWorker.markJobFailed(jobId, errorMessage);
        } catch (Exception e) {
            log.error("Error marking job as failed: {}", jobId, e);
        }
    }
}
