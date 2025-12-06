# Video Streaming Service - Torrent Integration Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [TorrentService API](#torrentservice-api)
5. [Download Workflow](#download-workflow)
6. [Monitoring & Debugging](#monitoring--debugging)
7. [Troubleshooting](#troubleshooting)

## Overview

The video-streaming service integrates the custom HyperTube Torrent Library to enable just-in-time video streaming via BitTorrent. The service manages torrent downloads, tracks progress, and automatically triggers format conversion for browser-compatible playback.

### Key Features

- **Progressive Downloading**: Sequential piece downloading optimized for streaming
- **Early Streaming**: Video playback starts at 10% buffer (configurable)
- **Auto-Conversion**: Automatic FFmpeg conversion for non-browser formats
- **Progress Tracking**: Real-time download statistics in database
- **Background Processing**: RabbitMQ-based job queue for non-blocking downloads
- **Multi-Torrent Support**: Concurrent downloads with separate DownloadManager instances
- **Resource Management**: Proper cleanup and shutdown procedures

## Architecture

### Component Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                    Video Streaming Service                        │
├──────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌─────────────────┐         ┌─────────────────┐                │
│  │ DownloadWorker  │◄────────│  RabbitMQ Queue │                │
│  │  (Listener)     │         │  "download"     │                │
│  └────────┬────────┘         └─────────────────┘                │
│           │                                                       │
│           │ processDownloadJob()                                 │
│           ▼                                                       │
│  ┌─────────────────┐         ┌─────────────────┐                │
│  │ TorrentService  │◄────────│  TrackerClient  │                │
│  │  (@Service)     │         │  (port 6881)    │                │
│  └────────┬────────┘         └─────────────────┘                │
│           │                                                       │
│           │ Manages                                               │
│           ▼                                                       │
│  ┌─────────────────┐         ┌─────────────────┐                │
│  │ DownloadManager │◄────────│  PeerConnection │                │
│  │  (per job)      │         │  (per peer)     │                │
│  └────────┬────────┘         └─────────────────┘                │
│           │                                                       │
│           │ On completion                                         │
│           ▼                                                       │
│  ┌─────────────────┐         ┌─────────────────┐                │
│  │  FFmpegService  │────────►│  ConversionQueue│                │
│  │  (if needed)    │         │  (RabbitMQ)     │                │
│  └─────────────────┘         └─────────────────┘                │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘
```

### Database Schema

**DownloadJob Entity:**
```sql
CREATE TABLE download_jobs (
    id UUID PRIMARY KEY,
    video_id UUID NOT NULL,
    torrent_id UUID NOT NULL,
    status VARCHAR(20),  -- PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
    progress INT,        -- 0-100
    download_speed BIGINT,  -- bytes/second
    eta_seconds INT,     -- estimated time remaining
    file_path VARCHAR(512),
    error_message VARCHAR(1024),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    completed_at TIMESTAMP
);
```

## Configuration

### Application Properties

```yaml
# application-docker.yml
streaming:
  storage:
    base-path: /var/hypertube/videos
    temp-path: /tmp/hypertube/temp
    subtitle-path: /var/hypertube/subtitles

  torrent:
    download-path: /tmp/hypertube/downloads
    max-download-speed: -1  # -1 = unlimited
    max-upload-speed: 100   # KB/s
    max-connections: 200
    dht-enabled: true
    port-range-start: 6881
    port-range-end: 6889

  conversion:
    enabled: true
    target-format: mp4
    target-codec: h264
    ffmpeg-path: ffmpeg
    ffprobe-path: ffprobe

  cache:
    ttl-days: 30
    cleanup-interval-hours: 6
```

### Environment Variables

```bash
# Docker environment
STREAMING_TORRENT_DOWNLOAD_PATH=/tmp/hypertube/downloads
STREAMING_TORRENT_MAX_CONNECTIONS=200
STREAMING_TORRENT_DHT_ENABLED=true
STREAMING_TORRENT_PORT_RANGE_START=6881
```

### TorrentService Configuration

```java
// TorrentService.java
private static final double BUFFER_THRESHOLD_PERCENT = 10.0;  // Start streaming at 10%

@PostConstruct
public void init() {
    // Download directory
    Path downloadPath = Paths.get(streamingConfig.getTorrent().getDownloadPath());
    Files.createDirectories(downloadPath);

    // Tracker client on configured port
    int port = streamingConfig.getTorrent().getPortRangeStart();
    trackerClient = new TrackerClient(port);
}
```

## TorrentService API

### Class: TorrentService

**Location**: `com.hypertube.streaming.service.TorrentService`

### Methods

#### startDownload

```java
public void startDownload(
    UUID jobId,
    UUID videoId,
    UUID torrentId,
    String magnetOrUrl,
    ProgressCallback progressCallback
)
```

**Description**: Starts a torrent download with progress tracking.

**Parameters:**
- `jobId`: Unique identifier for the download job
- `videoId`: Video entity ID
- `torrentId`: Torrent entity ID
- `magnetOrUrl`: Magnet link or .torrent URL
- `progressCallback`: Optional callback for progress updates

**Process:**
1. Parses torrent metadata
2. Creates download path: `/downloads/{videoId}/{sanitized-name}`
3. Initializes DownloadManager with SEQUENTIAL strategy
4. Starts download in background executor
5. Monitors progress every 2 seconds
6. Calls completion handler when done

**Example:**
```java
torrentService.startDownload(
    jobId,
    videoId,
    torrentId,
    "magnet:?xt=urn:btih:...",
    (id, progress, speed, eta) -> {
        log.info("Job {}: {}% @ {} KB/s", id, progress, speed/1024);
    }
);
```

#### isReadyForStreaming

```java
public boolean isReadyForStreaming(UUID jobId)
```

**Description**: Checks if download has reached the buffer threshold (10%).

**Returns**: `true` if progress >= 10%, `false` otherwise

**Example:**
```java
if (torrentService.isReadyForStreaming(jobId)) {
    startVideoPlayback(jobId);
}
```

#### getFilePath

```java
public String getFilePath(UUID jobId)
```

**Description**: Gets the file path for a download job.

**Returns**: Absolute path to downloaded file, or `null` if not available

**Example:**
```java
String path = torrentService.getFilePath(jobId);
if (path != null && Files.exists(Paths.get(path))) {
    streamVideo(path);
}
```

#### cancelDownload

```java
public void cancelDownload(UUID jobId)
```

**Description**: Cancels an active download and cleans up resources.

**Process:**
1. Stops DownloadManager
2. Removes from active downloads map
3. Updates database status to CANCELLED

**Example:**
```java
torrentService.cancelDownload(jobId);
```

### ProgressCallback Interface

```java
@FunctionalInterface
public interface ProgressCallback {
    void onProgress(UUID jobId, int progress, long downloadSpeed, int etaSeconds);
}
```

**Parameters:**
- `jobId`: Download job ID
- `progress`: Completion percentage (0-100)
- `downloadSpeed`: Current speed in bytes/second
- `etaSeconds`: Estimated time remaining in seconds (-1 if unknown)

## Download Workflow

### 1. User Initiates Download

```
User clicks "Watch" → Frontend calls API Gateway
    ↓
API Gateway → Search/Library Service → Creates DownloadJob
    ↓
RabbitMQ "download" queue receives DownloadMessage
```

### 2. Download Processing

```java
// DownloadWorker.java
@RabbitListener(queues = "${rabbitmq.queues.download}")
@Transactional
public void processDownloadJob(DownloadMessage message) {
    DownloadJob job = downloadJobRepository.findById(message.getJobId())
        .orElseThrow();

    // Update status to DOWNLOADING
    job.setStatus(DownloadJob.DownloadStatus.DOWNLOADING);
    downloadJobRepository.save(job);

    // Start torrent download
    torrentService.startDownload(
        job.getId(),
        message.getVideoId(),
        message.getTorrentId(),
        message.getMagnetLink(),
        this::updateJobProgress  // Progress callback
    );
}
```

### 3. Progress Updates

```java
// DownloadWorker.java
public void updateJobProgress(UUID jobId, int progress, long downloadSpeed, int etaSeconds) {
    downloadJobRepository.findById(jobId).ifPresent(job -> {
        job.setProgress(progress);
        job.setDownloadSpeed(downloadSpeed);
        job.setEtaSeconds(etaSeconds);
        job.setUpdatedAt(LocalDateTime.now());
        downloadJobRepository.save(job);
    });
}
```

### 4. Completion & Conversion

```java
// DownloadWorker.java
public void markJobCompleted(UUID jobId, String filePath) {
    downloadJobRepository.findById(jobId).ifPresent(job -> {
        job.setFilePath(filePath);
        job.setProgress(100);

        // Check if video needs conversion (MKV, AVI, etc.)
        if (ffmpegService.needsConversion(filePath)) {
            // Send to conversion queue
            ConversionMessage msg = ConversionMessage.builder()
                .jobId(jobId)
                .videoId(job.getVideoId())
                .inputFilePath(filePath)
                .outputFilePath(ffmpegService.generateOutputPath(filePath))
                .build();

            rabbitTemplate.convertAndSend(conversionQueue, msg);
            job.setStatus(DownloadJob.DownloadStatus.DOWNLOADING);  // Still processing
        } else {
            // No conversion needed (already MP4/WEBM)
            job.setStatus(DownloadJob.DownloadStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
        }

        downloadJobRepository.save(job);
    });
}
```

### 5. Error Handling

```java
// TorrentService.java
private void startDownload(...) {
    downloadExecutor.submit(() -> {
        try {
            TorrentMetadata metadata = parseTorrentMetadata(magnetOrUrl);
            // ... start download ...

        } catch (BencodeException e) {
            markJobFailed(jobId, "Invalid torrent format: " + e.getMessage());
        } catch (DownloadException e) {
            markJobFailed(jobId, "Download failed: " + e.getMessage());
        } catch (IOException e) {
            markJobFailed(jobId, "I/O error: " + e.getMessage());
        }
    });
}
```

## Monitoring & Debugging

### Log Messages

**Initialization:**
```
INFO  c.h.streaming.service.TorrentService : =================================================================
INFO  c.h.streaming.service.TorrentService : Initializing TorrentService with custom BitTorrent library
INFO  c.h.streaming.service.TorrentService : =================================================================
INFO  c.h.streaming.service.TorrentService : Service configuration:
INFO  c.h.streaming.service.TorrentService : - Download path: /tmp/hypertube/downloads
INFO  c.h.streaming.service.TorrentService : - Listening port: 6881
INFO  c.h.streaming.service.TorrentService : - Max connections: 200
INFO  c.h.streaming.service.TorrentService : - Buffer threshold: 10.0%
INFO  c.h.streaming.service.TorrentService : - DHT enabled: true
INFO  c.h.streaming.service.TorrentService : TorrentService initialized successfully
```

**Download Start:**
```
INFO  c.h.streaming.service.TorrentService : Starting torrent download for job: 123e4567-e89b-12d3-a456-426614174000
INFO  c.h.streaming.service.TorrentService : Video ID: aabbccdd-..., Torrent ID: eeffgghh-...
INFO  c.h.streaming.service.TorrentService : Magnet/URL: magnet:?xt=urn:btih:...
INFO  c.h.streaming.service.TorrentService : Parsed torrent: BigBuckBunny.mp4 (276445467 bytes, 1057 pieces)
INFO  c.h.streaming.service.TorrentService : Download started for job: 123e4567-...
```

**Progress Updates:**
```
DEBUG c.h.streaming.service.TorrentService : Job 123e4567: Progress=5.23%, Speed=2048 KB/s, Peers=7
DEBUG c.h.streaming.service.TorrentService : Job 123e4567: Progress=10.45%, Speed=3072 KB/s, Peers=12
INFO  c.h.streaming.service.TorrentService : Job 123e4567 ready for streaming (10% threshold reached)
```

**Completion:**
```
INFO  c.h.streaming.service.TorrentService : Download completed for job: 123e4567-...
INFO  c.h.streaming.worker.DownloadWorker : Video /downloads/123e4567/BigBuckBunny.mp4 needs conversion, sending to conversion queue
INFO  c.h.streaming.worker.DownloadWorker : Conversion job queued for: 123e4567-...
```

### Docker Logs

```bash
# Follow streaming service logs
docker logs -f hypertube-streaming-service

# Filter for torrent-related logs
docker logs hypertube-streaming-service 2>&1 | grep -i torrent

# Check last 100 lines
docker logs hypertube-streaming-service --tail 100

# Check download directory
docker exec hypertube-streaming-service ls -lh /tmp/hypertube/downloads
```

### Database Queries

```sql
-- Check active downloads
SELECT id, video_id, status, progress, download_speed, eta_seconds
FROM download_jobs
WHERE status = 'DOWNLOADING'
ORDER BY created_at DESC;

-- Check download success rate
SELECT status, COUNT(*) as count
FROM download_jobs
GROUP BY status;

-- Find failed downloads
SELECT id, video_id, error_message, created_at
FROM download_jobs
WHERE status = 'FAILED'
ORDER BY created_at DESC
LIMIT 10;

-- Average download time
SELECT AVG(EXTRACT(EPOCH FROM (completed_at - created_at))) as avg_seconds
FROM download_jobs
WHERE status = 'COMPLETED' AND completed_at IS NOT NULL;
```

### Health Checks

```bash
# Check service health
curl http://localhost:8083/actuator/health

# Check metrics
curl http://localhost:8083/actuator/metrics
curl http://localhost:8083/actuator/metrics/jvm.memory.used
```

## Troubleshooting

### Issue: Service Fails to Start

**Symptoms:**
```
ERROR c.h.streaming.service.TorrentService : Failed to initialize TorrentService
java.io.IOException: Permission denied
```

**Solutions:**
1. Check directory permissions
2. Ensure `/tmp/hypertube/downloads` is writable
3. Verify container user has correct permissions

```bash
docker exec hypertube-streaming-service ls -la /tmp/hypertube/
docker exec hypertube-streaming-service whoami
```

### Issue: Downloads Not Starting

**Symptoms:**
- Jobs stay in PENDING status
- No torrent-related logs

**Solutions:**
1. Check RabbitMQ connection
2. Verify DownloadWorker is listening to queue
3. Check for exceptions in logs

```bash
docker logs hypertube-rabbitmq
docker logs hypertube-streaming-service 2>&1 | grep -i "rabbit\|amqp"
```

### Issue: No Peers Found

**Symptoms:**
```
WARN  c.h.torrent.tracker.TrackerClient : No peers received from tracker
INFO  c.h.torrent.manager.DownloadManager : 0 active peers
```

**Solutions:**
1. Verify magnet link is valid and has seeders
2. Check network connectivity
3. Try different magnet link/torrent
4. Check firewall rules for port 6881

```bash
# Test tracker connectivity
curl "http://tracker.opentrackr.org:1337/announce?info_hash=..."

# Check if port is open
docker exec hypertube-streaming-service nc -zv tracker.opentrackr.org 1337
```

### Issue: Slow Download Speeds

**Symptoms:**
- Download speed < 100 KB/s
- Few active peers

**Solutions:**
1. Increase MAX_CONNECTIONS in configuration
2. Check network bandwidth
3. Use popular torrents with more seeders
4. Verify not being throttled by ISP

```java
// Increase connections
streaming.torrent.max-connections=500
```

### Issue: Downloads Fail with "Invalid torrent format"

**Symptoms:**
```
ERROR c.h.streaming.service.TorrentService : Failed to parse torrent metadata
com.hypertube.torrent.bencode.BencodeException: Invalid bencode data
```

**Solutions:**
1. Verify magnet link format
2. Check if .torrent file is corrupted
3. Ensure magnet link contains info hash (`xt=urn:btih:...`)

```java
// Valid magnet link format
magnet:?xt=urn:btih:<40-char-hex-hash>&dn=<name>&tr=<tracker-url>
```

### Issue: Files Not Found After Download

**Symptoms:**
- Download completes (100%)
- File path not found

**Solutions:**
1. Check download path configuration
2. Verify file wasn't moved/deleted
3. Check container volumes

```bash
# Find download files
docker exec hypertube-streaming-service find /tmp/hypertube/downloads -type f

# Check volume mounts
docker inspect hypertube-streaming-service | grep -A 10 Mounts
```

### Issue: Circular Dependency Error

**Symptoms:**
```
ERROR o.s.boot.SpringApplication : Application run failed
org.springframework.beans.factory.BeanCurrentlyInCreationException:
Error creating bean with name 'torrentService': Requested bean is currently in creation
```

**Solution:**
This is resolved by using `@Lazy` injection for DownloadWorker:

```java
public TorrentService(StreamingConfig streamingConfig,
                     DownloadJobRepository downloadJobRepository,
                     CachedVideoRepository cachedVideoRepository,
                     @Lazy DownloadWorker downloadWorker) {
    // @Lazy prevents circular dependency
    this.downloadWorker = downloadWorker;
}
```

## Best Practices

### Resource Management

```java
@PreDestroy
public void shutdown() {
    log.info("Shutting down TorrentService");

    // Stop all active downloads gracefully
    activeDownloads.values().forEach(manager -> {
        try {
            manager.stop();  // Sends STOPPED event to tracker
        } catch (Exception e) {
            log.warn("Error stopping download manager", e);
        }
    });

    // Shutdown executors
    progressMonitor.shutdown();
    downloadExecutor.shutdown();

    // Wait for graceful shutdown
    try {
        progressMonitor.awaitTermination(5, TimeUnit.SECONDS);
        downloadExecutor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

### Error Recovery

- Failed downloads are marked in database
- User can retry failed downloads
- Temporary network failures don't fail entire download
- Piece verification ensures data integrity

### Performance Optimization

1. **Sequential Strategy**: Optimal for streaming
2. **10% Buffer Threshold**: Balance between quick start and smooth playback
3. **Progress Monitoring**: 2-second interval balances responsiveness and overhead
4. **Connection Pooling**: Reuse tracker client across downloads

### Security Considerations

- Validate magnet links before parsing
- Sanitize filenames to prevent directory traversal
- Limit download directory access
- Monitor disk usage to prevent DoS

## API Integration Example

### Controller Endpoint

```java
@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    @Autowired
    private TorrentService torrentService;

    @GetMapping("/{videoId}/stream/status")
    public ResponseEntity<StreamStatus> getStreamStatus(@PathVariable UUID videoId) {
        DownloadJob job = downloadJobRepository.findByVideoId(videoId)
            .orElseThrow(() -> new NotFoundException("Video not found"));

        return ResponseEntity.ok(StreamStatus.builder()
            .ready(torrentService.isReadyForStreaming(job.getId()))
            .progress(job.getProgress())
            .downloadSpeed(job.getDownloadSpeed())
            .eta(job.getEtaSeconds())
            .filePath(torrentService.getFilePath(job.getId()))
            .build());
    }

    @PostMapping("/{videoId}/download/cancel")
    public ResponseEntity<Void> cancelDownload(@PathVariable UUID videoId) {
        DownloadJob job = downloadJobRepository.findByVideoId(videoId)
            .orElseThrow(() -> new NotFoundException("Video not found"));

        torrentService.cancelDownload(job.getId());
        return ResponseEntity.ok().build();
    }
}
```

## Future Enhancements

1. **DHT Support**: Implement DHT for tracker-less torrents
2. **Resume Downloads**: Support pausing and resuming
3. **Upload Seeding**: Seed completed files to support swarm health
4. **Magnet Link Resolution**: Fetch metadata from DHT/peers
5. **Multi-File Torrents**: Support torrents with multiple files
6. **Bandwidth Limits**: Per-download and global bandwidth limits
7. **Priority Downloads**: Queue management for multiple concurrent downloads

## References

- [BitTorrent Protocol Specification](http://www.bittorrent.org/beps/bep_0003.html)
- [Peer Wire Protocol](http://www.bittorrent.org/beps/bep_0003.html#peer-protocol)
- [Tracker HTTP Protocol](http://www.bittorrent.org/beps/bep_0003.html#trackers)
- [HyperTube Torrent Library Documentation](../torrent-lib/DOCUMENTATION.md)
- [Testing Guide](../../TESTING.md)
