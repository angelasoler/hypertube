# Local Testing Guide - Torrent Download Functionality

This guide walks you through testing the custom BitTorrent library integration in the video-streaming service.

## Prerequisites

- Docker and Docker Compose installed
- At least 5GB free disk space
- Ports 8080-8084, 5432, 6379, 5672, 8761 available

## Quick Start

### 1. Build the Services

The torrent-lib will be automatically built and installed during the video-streaming service build:

```bash
# Build only the streaming service (includes torrent-lib)
docker-compose build streaming-service

# Or build all services
docker-compose build
```

### 2. Start Required Services

```bash
# Start all services
docker-compose up -d

# Or start only what's needed for streaming
docker-compose up -d postgres redis rabbitmq eureka-server streaming-service
```

### 3. Verify Service Health

```bash
# Check service status
docker-compose ps

# Check streaming service logs
docker logs hypertube-streaming-service

# Look for torrent initialization messages
docker logs hypertube-streaming-service | grep -i "torrent"
```

You should see logs like:
```
Initializing TorrentService with custom BitTorrent library
Service configuration:
- Download path: /tmp/hypertube/downloads
- Listening port: 6881
- Max connections: 200
- Buffer threshold: 10.0%
- DHT enabled: true
TorrentService initialized successfully
```

### 4. Run the Test Script

```bash
./test-torrent-download.sh
```

## Manual Testing

### Test with Big Buck Bunny (Creative Commons)

This is a small, legal torrent perfect for testing:

**Magnet Link:**
```
magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Fexplodie.org%3A6969
```

### Create a Download Job

You'll need to create a download job through the API. First, you need to:

1. **Register/Login** to get an auth token
2. **Search** for a video to get video metadata
3. **Create** a download job with the magnet link

Example using curl (you'll need valid auth token):

```bash
# Create a download job
curl -X POST http://localhost:8083/api/v1/download \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "videoId": "00000000-0000-0000-0000-000000000001",
    "torrentId": "00000000-0000-0000-0000-000000000002",
    "magnetLink": "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny"
  }'
```

### Monitor Download Progress

```bash
# Follow service logs in real-time
docker logs -f hypertube-streaming-service

# Check download directory
docker exec hypertube-streaming-service ls -lh /tmp/hypertube/downloads

# Check database for job status
docker exec -it hypertube-postgres psql -U hypertube_user -d hypertube -c "SELECT id, status, progress, download_speed FROM download_jobs ORDER BY created_at DESC LIMIT 5;"
```

### Expected Log Output

During download, you should see:
```
Starting torrent download for job: <job-id>
Parsed torrent: Big Buck Bunny (XXXXX bytes, XX pieces)
Download started for job: <job-id>
Contacting tracker: http://tracker.example.com/announce
Received XX peers from tracker
Connected to peer: 192.168.1.100:6881
Downloading piece 0 of XX
Job <job-id>: Progress=5.50%, Speed=1024 KB/s, Peers=3
Job <job-id>: Progress=10.00%, Speed=2048 KB/s, Peers=5
Ready for streaming (10% buffer threshold reached)
```

## Troubleshooting

### Service won't start

```bash
# Check all service logs
docker-compose logs

# Rebuild from scratch
docker-compose down -v
docker-compose build --no-cache streaming-service
docker-compose up -d
```

### No peers found

This is expected with some torrents. The torrent library will:
1. Contact all trackers in the magnet link
2. Wait for peer announcements
3. Retry periodically

If no peers are available, the download will remain at 0% until peers appear.

### Build fails with "torrent-lib not found"

Make sure the build context is correct in docker-compose.yml:
```yaml
streaming-service:
  build:
    context: ./services
    dockerfile: video-streaming/Dockerfile
```

### Permission errors on download directory

The container runs as non-root user `spring`. Check directory permissions:
```bash
docker exec hypertube-streaming-service ls -la /tmp/hypertube/
```

## Performance Testing

### Metrics to Monitor

1. **Download Speed**: Should see KB/s in logs
2. **Peer Count**: Number of active connections
3. **Progress**: Percentage complete
4. **Buffer Threshold**: Should reach 10% quickly for streaming
5. **Piece Verification**: SHA-1 hash checks

### Check Resource Usage

```bash
# CPU and memory usage
docker stats hypertube-streaming-service

# Disk usage
docker exec hypertube-streaming-service df -h /tmp/hypertube/
```

## Testing Different Scenarios

### 1. Sequential Download (Streaming Optimized)
- Default strategy: `PieceSelectionStrategy.SEQUENTIAL`
- Downloads pieces in order from start to finish
- Best for video streaming

### 2. Buffer Threshold
- Streaming starts when 10% downloaded
- Check `isReadyForStreaming()` endpoint

### 3. Multiple Concurrent Downloads
- Service supports multiple simultaneous jobs
- Each uses separate DownloadManager instance
- Monitor with: `docker exec hypertube-streaming-service ls /tmp/hypertube/downloads`

### 4. Format Conversion
- After download completes, DownloadWorker triggers FFmpeg conversion
- Check logs for: "Video needs conversion"
- Final status should be COMPLETED after conversion

## Clean Up

```bash
# Stop all services
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose down -v

# Remove downloaded videos
docker volume rm hypertube_video_cache
```

## Next Steps

After successful testing:
1. Test with different video formats (MKV, AVI, MP4)
2. Test with large files (>1GB)
3. Test cancellation of downloads
4. Test resume functionality
5. Load testing with multiple concurrent downloads
