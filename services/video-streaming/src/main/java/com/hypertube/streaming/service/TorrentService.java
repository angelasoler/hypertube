package com.hypertube.streaming.service;

import com.hypertube.streaming.config.StreamingConfig;
import com.hypertube.streaming.entity.DownloadJob;
import com.hypertube.streaming.entity.DownloadJob.DownloadStatus;
import com.hypertube.streaming.repository.DownloadJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

/**
 * TorrentService - Handles BitTorrent downloads for video streaming
 *
 * NOTE: This is a placeholder implementation. The actual torrent functionality
 * requires libtorrent4j which needs to be built from source or obtained from Jitpack.
 *
 * Future implementation should use a low-level torrent library (NOT webtorrent, pulsar, or peerflix)
 * as specified in the project requirements.
 */
@Service
@Slf4j
public class TorrentService {

    private final StreamingConfig streamingConfig;
    private final DownloadJobRepository downloadJobRepository;

    public TorrentService(StreamingConfig streamingConfig, DownloadJobRepository downloadJobRepository) {
        this.streamingConfig = streamingConfig;
        this.downloadJobRepository = downloadJobRepository;
    }

    @PostConstruct
    public void init() {
        log.info("TorrentService initialized (placeholder implementation)");
        log.warn("Torrent download functionality requires libtorrent4j to be configured");
    }

    public void startDownload(UUID jobId, String magnetLink) {
        DownloadJob job = downloadJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Download job not found: " + jobId));

        log.info("Torrent download requested for job: {} (placeholder - not implemented)", jobId);

        job.setStatus(DownloadStatus.PENDING);
        job.setProgress(0);
        job.setErrorMessage("Torrent download not yet implemented - requires libtorrent4j configuration");
        downloadJobRepository.save(job);
    }

    public void cancelDownload(UUID jobId) {
        DownloadJob job = downloadJobRepository.findById(jobId).orElse(null);
        if (job != null) {
            job.setStatus(DownloadStatus.CANCELLED);
            downloadJobRepository.save(job);
            log.info("Cancelled download for job: {}", jobId);
        }
    }
}
