package com.hypertube.streaming.service;

import com.hypertube.streaming.entity.CachedVideo;
import com.hypertube.streaming.entity.DownloadJob;
import com.hypertube.streaming.repository.CachedVideoRepository;
import com.hypertube.streaming.repository.DownloadJobRepository;
import com.hypertube.streaming.util.HttpRangeParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Service for streaming video files with HTTP Range request support (RFC 7233).
 *
 * Enables:
 * - Progressive video playback in browsers
 * - Video seeking during active downloads
 * - Efficient bandwidth usage with partial content delivery
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoStreamingService {

    private final DownloadJobRepository downloadJobRepository;
    private final CachedVideoRepository cachedVideoRepository;
    private final TorrentService torrentService;

    private static final int DEFAULT_CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    /**
     * Streams a video file with support for HTTP Range requests.
     *
     * @param jobId The download job ID
     * @param rangeHeader The HTTP Range header value (optional)
     * @return ResponseEntity with video content and appropriate headers
     */
    public ResponseEntity<Resource> streamVideo(UUID jobId, String rangeHeader) {
        try {
            // Find the download job
            DownloadJob job = downloadJobRepository.findById(jobId)
                    .orElse(null);

            if (job == null) {
                log.warn("Download job not found: {}", jobId);
                return ResponseEntity.notFound().build();
            }

            // Get file path
            String filePath = getVideoFilePath(job);
            if (filePath == null || filePath.isEmpty()) {
                log.warn("File path not available for job: {}", jobId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            File videoFile = new File(filePath);
            if (!videoFile.exists()) {
                log.warn("Video file not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            long fileSize = videoFile.length();
            String contentType = detectContentType(videoFile);

            // Handle range request
            if (rangeHeader != null && !rangeHeader.isEmpty()) {
                return handleRangeRequest(videoFile, rangeHeader, fileSize, contentType);
            } else {
                return handleFullRequest(videoFile, fileSize, contentType);
            }

        } catch (Exception e) {
            log.error("Error streaming video for job: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handles a full file request (no Range header).
     */
    private ResponseEntity<Resource> handleFullRequest(File videoFile, long fileSize, String contentType) {
        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(videoFile));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileSize);
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

            log.info("Serving full file: {} (size: {} bytes)", videoFile.getName(), fileSize);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            log.error("Error reading video file: {}", videoFile.getPath(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handles a partial content request with Range header (RFC 7233).
     */
    private ResponseEntity<Resource> handleRangeRequest(File videoFile, String rangeHeader,
                                                        long fileSize, String contentType) {
        try {
            // Parse range header
            List<HttpRangeParser.Range> ranges = HttpRangeParser.parseRangeHeader(rangeHeader, fileSize);

            if (ranges.isEmpty()) {
                log.warn("Invalid or unsatisfiable range request: {}", rangeHeader);
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }

            // For simplicity, we only support single range requests
            // Multi-range requests would require multipart/byteranges response
            if (ranges.size() > 1) {
                log.warn("Multiple range requests not supported, using first range");
            }

            HttpRangeParser.Range range = ranges.get(0);

            // Read the requested range
            byte[] data = readFileRange(videoFile, range.getStart(), range.getEnd());

            if (data == null) {
                log.error("Failed to read file range: {}-{}", range.getStart(), range.getEnd());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Build response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(data.length);
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CONTENT_RANGE,
                    HttpRangeParser.generateContentRangeHeader(range, fileSize));
            headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

            log.info("Serving partial content: {} bytes {}-{}/{} ({})",
                    videoFile.getName(), range.getStart(), range.getEnd(), fileSize, data.length);

            InputStreamResource resource = new InputStreamResource(
                    new java.io.ByteArrayInputStream(data));

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error handling range request: {}", rangeHeader, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reads a specific byte range from a file.
     *
     * @param file The file to read from
     * @param start Start position (inclusive)
     * @param end End position (inclusive)
     * @return Byte array containing the requested range, or null on error
     */
    private byte[] readFileRange(File file, long start, long end) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            long rangeLength = end - start + 1;

            // Limit range length to prevent memory issues
            if (rangeLength > Integer.MAX_VALUE) {
                log.warn("Range too large: {} bytes, limiting to {}", rangeLength, Integer.MAX_VALUE);
                rangeLength = Integer.MAX_VALUE;
            }

            byte[] buffer = new byte[(int) rangeLength];
            randomAccessFile.seek(start);
            int bytesRead = randomAccessFile.read(buffer);

            if (bytesRead != rangeLength) {
                log.warn("Expected to read {} bytes but read {}", rangeLength, bytesRead);
            }

            return buffer;

        } catch (IOException e) {
            log.error("Error reading file range {}-{}: {}", start, end, e.getMessage());
            return null;
        }
    }

    /**
     * Gets the video file path for a download job.
     */
    private String getVideoFilePath(DownloadJob job) {
        // First check if job has a file path
        if (job.getFilePath() != null && !job.getFilePath().isEmpty()) {
            return job.getFilePath();
        }

        // Try to get from TorrentService
        String filePath = torrentService.getFilePath(job.getId());
        if (filePath != null && !filePath.isEmpty()) {
            return filePath;
        }

        // Check cached videos
        CachedVideo cachedVideo = cachedVideoRepository
                .findByVideoId(job.getVideoId())
                .stream()
                .findFirst()
                .orElse(null);

        if (cachedVideo != null) {
            return cachedVideo.getFilePath();
        }

        return null;
    }

    /**
     * Detects the MIME content type of a video file.
     */
    private String detectContentType(File file) {
        try {
            Path path = Paths.get(file.getAbsolutePath());
            String contentType = Files.probeContentType(path);

            if (contentType != null) {
                return contentType;
            }
        } catch (IOException e) {
            log.warn("Error detecting content type for {}: {}", file.getName(), e.getMessage());
        }

        // Fallback based on file extension
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileName.endsWith(".webm")) {
            return "video/webm";
        } else if (fileName.endsWith(".mkv")) {
            return "video/x-matroska";
        } else if (fileName.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (fileName.endsWith(".mov")) {
            return "video/quicktime";
        }

        // Default to octet-stream
        return "application/octet-stream";
    }

    /**
     * Checks if a video is ready for streaming (buffer threshold reached).
     */
    public boolean isReadyForStreaming(UUID jobId) {
        return torrentService.isReadyForStreaming(jobId);
    }
}
