package com.hypertube.streaming.worker;

import com.hypertube.streaming.dto.DownloadMessage;
import com.hypertube.streaming.entity.DownloadJob;
import com.hypertube.streaming.repository.DownloadJobRepository;
import com.hypertube.streaming.service.TorrentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Worker that processes video download jobs from the RabbitMQ queue.
 * Handles torrent downloads with progressive streaming support.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DownloadWorker {

    private final DownloadJobRepository downloadJobRepository;
    private final TorrentService torrentService;

    /**
     * Listens to the download queue and processes download job messages.
     *
     * @param message The download job message containing video and torrent details
     */
    @RabbitListener(queues = "${rabbitmq.queues.download}")
    @Transactional
    public void processDownloadJob(DownloadMessage message) {
        log.info("Received download job for video ID: {}, torrent ID: {}",
                message.getVideoId(), message.getTorrentId());

        DownloadJob job = null;
        try {
            // Find the download job
            job = downloadJobRepository.findById(message.getJobId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Download job not found: " + message.getJobId()));

            // Update job status to DOWNLOADING
            job.setStatus(DownloadJob.DownloadStatus.DOWNLOADING);
            job.setUpdatedAt(LocalDateTime.now());
            downloadJobRepository.save(job);

            log.info("Starting torrent download for job: {}", job.getId());

            // Start torrent download using TorrentService
            torrentService.startDownload(
                    job.getId(),
                    message.getVideoId(),
                    message.getTorrentId(),
                    message.getMagnetLink() != null ? message.getMagnetLink() : message.getTorrentUrl(),
                    this::updateJobProgress
            );

            log.info("Successfully started download for job: {}", job.getId());

        } catch (Exception e) {
            log.error("Error processing download job: {}", message.getJobId(), e);

            if (job != null) {
                job.setStatus(DownloadJob.DownloadStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                job.setUpdatedAt(LocalDateTime.now());
                downloadJobRepository.save(job);
            }
        }
    }

    /**
     * Callback method to update job progress during download.
     *
     * @param jobId The job ID
     * @param progress The download progress (0-100)
     * @param downloadSpeed The current download speed in bytes/second
     * @param etaSeconds The estimated time remaining in seconds
     */
    public void updateJobProgress(UUID jobId, int progress, long downloadSpeed, int etaSeconds) {
        try {
            downloadJobRepository.findById(jobId).ifPresent(job -> {
                job.setProgress(progress);
                job.setDownloadSpeed(downloadSpeed);
                job.setEtaSeconds(etaSeconds);
                job.setUpdatedAt(LocalDateTime.now());
                downloadJobRepository.save(job);
            });
        } catch (Exception e) {
            log.error("Error updating job progress for job: {}", jobId, e);
        }
    }

    /**
     * Marks a download job as completed.
     *
     * @param jobId The job ID
     * @param filePath The path to the downloaded file
     */
    public void markJobCompleted(UUID jobId, String filePath) {
        try {
            downloadJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(DownloadJob.DownloadStatus.COMPLETED);
                job.setProgress(100);
                job.setFilePath(filePath);
                job.setUpdatedAt(LocalDateTime.now());
                downloadJobRepository.save(job);
                log.info("Download job completed: {}", jobId);
            });
        } catch (Exception e) {
            log.error("Error marking job as completed: {}", jobId, e);
        }
    }

    /**
     * Marks a download job as failed.
     *
     * @param jobId The job ID
     * @param errorMessage The error message
     */
    public void markJobFailed(UUID jobId, String errorMessage) {
        try {
            downloadJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(DownloadJob.DownloadStatus.FAILED);
                job.setErrorMessage(errorMessage);
                job.setUpdatedAt(LocalDateTime.now());
                downloadJobRepository.save(job);
                log.error("Download job failed: {} - {}", jobId, errorMessage);
            });
        } catch (Exception e) {
            log.error("Error marking job as failed: {}", jobId, e);
        }
    }
}
