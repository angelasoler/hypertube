package com.hypertube.streaming.worker;

import com.hypertube.streaming.dto.ConversionMessage;
import com.hypertube.streaming.entity.DownloadJob;
import com.hypertube.streaming.repository.DownloadJobRepository;
import com.hypertube.streaming.service.FFmpegService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Worker that processes video conversion jobs from the RabbitMQ queue.
 * Converts non-browser-compatible video formats to MP4 using FFmpeg.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConversionWorker {

    private final DownloadJobRepository downloadJobRepository;
    private final FFmpegService ffmpegService;

    /**
     * Listens to the conversion queue and processes conversion job messages.
     *
     * @param message The conversion job message containing file paths and job details
     */
    @RabbitListener(queues = "${rabbitmq.queues.conversion}")
    @Transactional
    public void processConversionJob(ConversionMessage message) {
        log.info("Received conversion job for job ID: {}", message.getJobId());
        log.info("Input file: {}", message.getInputFilePath());

        DownloadJob job = null;
        try {
            // Find the download job
            job = downloadJobRepository.findById(message.getJobId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Download job not found: " + message.getJobId()));

            // Check if FFmpeg is available
            if (!ffmpegService.isFFmpegAvailable()) {
                throw new RuntimeException("FFmpeg is not available on this system");
            }

            // Check if conversion is needed
            if (!ffmpegService.needsConversion(message.getInputFilePath())) {
                log.info("File {} does not need conversion, skipping", message.getInputFilePath());
                job.setStatus(DownloadJob.DownloadStatus.COMPLETED);
                job.setUpdatedAt(LocalDateTime.now());
                downloadJobRepository.save(job);
                return;
            }

            // Update job status to CONVERTING
            job.setStatus(DownloadJob.DownloadStatus.CONVERTING);
            job.setUpdatedAt(LocalDateTime.now());
            downloadJobRepository.save(job);

            log.info("Starting conversion for job: {}", job.getId());

            // Generate output path
            String outputPath = message.getOutputFilePath() != null
                    ? message.getOutputFilePath()
                    : ffmpegService.generateOutputPath(message.getInputFilePath());

            log.info("Output file: {}", outputPath);

            // Perform conversion
            boolean success = ffmpegService.convertToMp4(message.getInputFilePath(), outputPath);

            if (success) {
                // Update job with converted file path
                job.setStatus(DownloadJob.DownloadStatus.COMPLETED);
                job.setFilePath(outputPath);
                job.setProgress(100);
                job.setCompletedAt(LocalDateTime.now());
                job.setUpdatedAt(LocalDateTime.now());
                downloadJobRepository.save(job);

                log.info("Conversion completed successfully for job: {}", job.getId());
                log.info("Converted file available at: {}", outputPath);
            } else {
                throw new RuntimeException("FFmpeg conversion failed");
            }

        } catch (Exception e) {
            log.error("Error processing conversion job: {}", message.getJobId(), e);

            if (job != null) {
                job.setStatus(DownloadJob.DownloadStatus.FAILED);
                job.setErrorMessage("Conversion failed: " + e.getMessage());
                job.setUpdatedAt(LocalDateTime.now());
                downloadJobRepository.save(job);
            }
        }
    }
}
