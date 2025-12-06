# HyperTube Torrent Library - Complete Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [API Reference](#api-reference)
5. [Usage Examples](#usage-examples)
6. [Configuration](#configuration)
7. [Testing](#testing)
8. [Troubleshooting](#troubleshooting)

## Overview

The HyperTube Torrent Library is a pure-Java implementation of the BitTorrent protocol, specifically optimized for video streaming applications. It provides a complete BitTorrent client with no external native dependencies.

### Key Features

- **Pure Java Implementation**: No JNI or native libraries required
- **Streaming Optimized**: Sequential piece downloading for immediate playback
- **Multi-Peer Support**: Concurrent downloads from multiple peers
- **SHA-1 Verification**: Automatic piece integrity checking
- **Tracker Support**: HTTP/HTTPS tracker communication
- **Magnet Link Support**: Parse and download from magnet URIs
- **Progress Tracking**: Real-time download statistics and callbacks
- **Configurable Strategies**: Sequential or rarest-first piece selection

### Requirements

- Java 17 or higher
- Maven 3.9+
- Network connectivity for tracker communication and peer connections

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     DownloadManager                          │
│  - Orchestrates multi-peer downloads                        │
│  - Manages piece selection strategy                         │
│  - Tracks progress and statistics                           │
└────────┬────────────────────────────────────┬───────────────┘
         │                                    │
         │                                    │
┌────────▼─────────┐                 ┌────────▼─────────────┐
│  TrackerClient   │                 │   PeerConnection     │
│  - HTTP tracker  │                 │   - Handshake        │
│  - Peer discovery│                 │   - Message protocol │
│  - Announcements │                 │   - Block requests   │
└──────────────────┘                 └──────────────────────┘
         │                                    │
         │                                    │
┌────────▼─────────┐                 ┌────────▼─────────────┐
│ TorrentMetadata  │                 │       Piece          │
│  - .torrent parse│                 │   - 16KB blocks      │
│  - Magnet links  │                 │   - SHA-1 verify     │
│  - Info hash     │                 │   - Data management  │
└──────────────────┘                 └──────────────────────┘
         │                                    │
         │                                    │
┌────────▼─────────┐                 ┌────────▼─────────────┐
│ BencodeDecoder   │                 │      Bitfield        │
│ BencodeEncoder   │                 │   - Progress track   │
│  - Binary format │                 │   - Piece status     │
└──────────────────┘                 └──────────────────────┘
```

### Data Flow

1. **Parse Torrent**: `TorrentMetadata` parses .torrent file or magnet link
2. **Contact Tracker**: `TrackerClient` announces and receives peer list
3. **Connect to Peers**: `PeerConnection` performs handshake with each peer
4. **Select Pieces**: `DownloadManager` chooses pieces based on strategy
5. **Download Blocks**: `PeerConnection` requests 16KB blocks
6. **Assemble Piece**: `Piece` combines blocks and verifies SHA-1 hash
7. **Write to Disk**: `DownloadManager` writes verified piece to file
8. **Update Progress**: `Bitfield` tracks completion percentage

## Core Components

### 1. TorrentMetadata

Parses .torrent files and magnet links to extract torrent information.

**Key Methods:**
```java
// Parse from .torrent file
public static TorrentMetadata fromTorrentFile(byte[] torrentData)
    throws BencodeException

// Parse from magnet link
public static TorrentMetadata fromMagnetLink(String magnetLink)
    throws BencodeException

// Get torrent properties
public String getName()
public long getTotalSize()
public int getNumPieces()
public long getPieceLength()
public String getInfoHashHex()
public byte[] getInfoHash()
public List<String> getTrackers()
public byte[] getPieceHash(int pieceIndex)
public int getPieceLength(int pieceIndex)
```

**Supported Formats:**
- Standard .torrent files (bencode format)
- Magnet links with `xt=urn:btih:` info hash
- Multi-tracker torrents
- Single-file torrents (multi-file support coming)

### 2. TrackerClient

Communicates with BitTorrent trackers to discover peers.

**Key Methods:**
```java
// Announce to tracker
public TrackerResponse announce(
    TorrentMetadata metadata,
    TrackerEvent event,  // STARTED, COMPLETED, STOPPED
    long downloaded,
    long uploaded,
    long left
) throws TrackerException

// Get generated peer ID
public byte[] getPeerId()
```

**TrackerResponse Fields:**
- `int interval`: Seconds until next announce
- `int seeders`: Number of seeders in swarm
- `int leechers`: Number of leechers in swarm
- `List<Peer> peers`: List of available peers

**Peer ID Format:**
- Format: `-HT0100-<12 random bytes>`
- HT = HyperTube client
- 0100 = Version 1.0.0

### 3. PeerConnection

Manages connection to a single peer and handles the peer wire protocol.

**Key Methods:**
```java
// Establish connection and perform handshake
public void connect() throws PeerException

// Send messages
public synchronized void sendMessage(PeerMessage message)

// Receive messages (blocking)
public PeerMessage receiveMessage() throws PeerException

// Request a block
public void requestBlock(int pieceIndex, int begin, int length)

// Check if peer has a piece
public boolean hasPiece(int pieceIndex)

// Connection status
public boolean isConnected()
public synchronized boolean isPeerChoking()
public synchronized boolean isAmChoking()
```

**Handshake Protocol:**
```
<1 byte pstrlen = 19>
<19 bytes pstr = "BitTorrent protocol">
<8 bytes reserved = 0x00>
<20 bytes info_hash>
<20 bytes peer_id>
Total: 68 bytes
```

### 4. PeerMessage

Represents BitTorrent peer wire protocol messages.

**Message Types:**
```java
public enum MessageType {
    CHOKE(0),           // Stop sending data
    UNCHOKE(1),         // Resume sending data
    INTERESTED(2),      // Want to download
    NOT_INTERESTED(3),  // Don't want to download
    HAVE(4),            // Announce piece availability
    BITFIELD(5),        // Send piece availability bitmap
    REQUEST(6),         // Request a block
    PIECE(7),           // Send block data
    CANCEL(8),          // Cancel block request
    KEEP_ALIVE(-1)      // Keep connection alive
}
```

**Factory Methods:**
```java
public static PeerMessage choke()
public static PeerMessage unchoke()
public static PeerMessage interested()
public static PeerMessage notInterested()
public static PeerMessage have(int pieceIndex)
public static PeerMessage bitfield(byte[] bitfield)
public static PeerMessage request(int pieceIndex, int begin, int length)
public static PeerMessage piece(int pieceIndex, int begin, byte[] block)
public static PeerMessage cancel(int pieceIndex, int begin, int length)
public static PeerMessage keepAlive()
```

**Message Format:**
```
<4 bytes length>
<1 byte ID>
<variable payload>
```

### 5. Piece

Manages downloading and verification of a single piece.

**Key Methods:**
```java
// Write a 16KB block
public synchronized boolean writeBlock(int begin, byte[] blockData)

// Check if all blocks downloaded
public boolean isComplete()

// Verify SHA-1 hash
public boolean verify()

// Get next block to request
public BlockRequest getNextBlockRequest()

// Get piece data
public byte[] getData()
```

**Block Management:**
- Default block size: 16,384 bytes (16 KB)
- Pieces are divided into blocks for efficient transfer
- Last block may be smaller
- SHA-1 verification after all blocks received

### 6. Bitfield

Tracks which pieces have been downloaded.

**Key Methods:**
```java
// Mark piece as downloaded
public void setPiece(int pieceIndex)

// Check if we have a piece
public boolean hasPiece(int pieceIndex)

// Check if download is complete
public boolean isComplete()

// Get completion percentage
public double getCompletionPercentage()

// Find next missing piece
public int getNextMissingPiece()
public int getNextMissingPiece(Bitfield peerBitfield)
```

### 7. DownloadManager

Orchestrates the entire download process across multiple peers.

**Key Methods:**
```java
// Start the download
public void start() throws IOException, DownloadException

// Stop the download
public void stop()

// Add a peer to the download
public void addPeer(Peer peer)

// Progress tracking
public boolean isComplete()
public double getProgress()  // Returns 0.0 to 100.0
public AtomicLong getDownloaded()
public AtomicLong getUploaded()
public long getDownloadSpeed()  // Bytes per second
public int getActivePeerCount()
```

**Constructor:**
```java
public DownloadManager(
    TorrentMetadata metadata,
    TrackerClient trackerClient,
    Path downloadPath,
    PieceSelectionStrategy strategy
)
```

### 8. PieceSelectionStrategy

Determines the order in which pieces are downloaded.

**Strategies:**
```java
public enum PieceSelectionStrategy {
    SEQUENTIAL,      // Download pieces in order (best for streaming)
    RAREST_FIRST     // Download rarest pieces first (better for swarm health)
}
```

**Recommendations:**
- **SEQUENTIAL**: Use for video streaming to enable early playback
- **RAREST_FIRST**: Use for general downloads to improve torrent health

## API Reference

### Quick Start Example

```java
// 1. Parse magnet link
String magnetLink = "magnet:?xt=urn:btih:...";
TorrentMetadata metadata = TorrentMetadata.fromMagnetLink(magnetLink);

// 2. Create tracker client
TrackerClient tracker = new TrackerClient(6881); // Listen on port 6881

// 3. Create download path
Path downloadPath = Paths.get("/downloads/video.mp4");

// 4. Create download manager with sequential strategy
DownloadManager manager = new DownloadManager(
    metadata,
    tracker,
    downloadPath,
    PieceSelectionStrategy.SEQUENTIAL
);

// 5. Start download
manager.start();

// 6. Monitor progress
while (!manager.isComplete()) {
    System.out.printf("Progress: %.2f%%\n", manager.getProgress());
    System.out.printf("Speed: %d KB/s\n", manager.getDownloadSpeed() / 1024);
    System.out.printf("Peers: %d\n", manager.getActivePeerCount());
    Thread.sleep(2000);
}

// 7. Stop manager
manager.stop();
System.out.println("Download complete!");
```

### Advanced Usage

#### Custom Progress Callback

```java
DownloadManager manager = new DownloadManager(...);

// Create progress monitoring thread
ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
monitor.scheduleAtFixedRate(() -> {
    double progress = manager.getProgress();
    long speed = manager.getDownloadSpeed();
    int peers = manager.getActivePeerCount();

    // Update UI or database
    updateProgressUI(progress, speed, peers);

    // Check streaming threshold
    if (progress >= 10.0) {
        startVideoPlayback();
    }
}, 0, 2, TimeUnit.SECONDS);

manager.start();
```

#### Manual Peer Management

```java
DownloadManager manager = new DownloadManager(...);
manager.start();

// Add specific peers manually
Peer peer1 = new Peer("192.168.1.100", 6881, null);
Peer peer2 = new Peer("192.168.1.101", 6881, null);

manager.addPeer(peer1);
manager.addPeer(peer2);
```

#### Reading .torrent Files

```java
// Read .torrent file
Path torrentFile = Paths.get("/path/to/file.torrent");
byte[] torrentData = Files.readAllBytes(torrentFile);

// Parse metadata
TorrentMetadata metadata = TorrentMetadata.fromTorrentFile(torrentData);

System.out.println("Name: " + metadata.getName());
System.out.println("Size: " + metadata.getTotalSize() + " bytes");
System.out.println("Pieces: " + metadata.getNumPieces());
System.out.println("Info Hash: " + metadata.getInfoHashHex());
System.out.println("Trackers: " + metadata.getTrackers());
```

## Configuration

### Download Manager Configuration

The `DownloadManager` behavior can be configured through several parameters:

```java
// In DownloadManager.java
private static final int MAX_CONNECTIONS = 10;  // Max concurrent peer connections
private static final int TRACKER_ANNOUNCE_INTERVAL = 1800;  // Seconds between announces
private static final int KEEP_ALIVE_INTERVAL = 120;  // Seconds between keep-alive messages
```

### Buffer Threshold for Streaming

```java
// In TorrentService.java (integration example)
private static final double BUFFER_THRESHOLD_PERCENT = 10.0;  // Start streaming at 10%
```

### Port Configuration

```java
// TrackerClient listens on a specific port for incoming connections
TrackerClient tracker = new TrackerClient(6881);  // BitTorrent default

// Use different ports for multiple concurrent downloads
TrackerClient tracker1 = new TrackerClient(6881);
TrackerClient tracker2 = new TrackerClient(6882);
```

## Testing

### Running Unit Tests

```bash
cd services/torrent-lib
mvn test
```

### Test Coverage

Current test coverage: **87%** (81/93 tests passing)

**Test Suites:**
- `BencodeDecoderTest`: 30 tests - Bencode decoding edge cases
- `BencodeEncoderTest`: 15 tests - Bencode encoding
- `TorrentMetadataTest`: 12 tests - Torrent parsing and magnet links
- `TrackerClientTest`: 8 tests - Tracker communication
- `PeerMessageTest`: 16 tests - Peer wire protocol
- `PieceTest`: 13 tests - Piece and block management
- `BitfieldTest`: 14 tests - Progress tracking
- `DownloadManagerTest`: 14 tests - Download orchestration

### Integration Testing

See `TESTING.md` in the project root for end-to-end integration testing with the video-streaming service.

## Troubleshooting

### Common Issues

#### 1. No Peers Found

**Symptoms**: Download starts but no peers connect

**Solutions:**
- Check if magnet link/torrent has active trackers
- Verify network connectivity
- Try a different port (avoid blocked ports like 6889)
- Check firewall settings

**Example:**
```java
TrackerResponse response = tracker.announce(metadata, TrackerEvent.STARTED, 0, 0, metadata.getTotalSize());
if (response.getPeers().isEmpty()) {
    log.warn("No peers found. Seeders: {}, Leechers: {}",
        response.getSeeders(), response.getLeechers());
}
```

#### 2. Piece Verification Failures

**Symptoms**: Downloaded pieces fail SHA-1 verification

**Solutions:**
- Ensure full piece is downloaded before verification
- Check for data corruption during transfer
- Verify metadata info hash is correct

**Example:**
```java
Piece piece = new Piece(0, pieceLength, expectedHash);
piece.writeBlock(0, blockData);
if (!piece.verify()) {
    log.error("Piece {} verification failed, re-downloading", 0);
}
```

#### 3. Connection Timeouts

**Symptoms**: Peers disconnect frequently

**Solutions:**
- Implement keep-alive messages
- Check network stability
- Increase connection timeout

**Example:**
```java
// Send keep-alive every 2 minutes
PeerMessage keepAlive = PeerMessage.keepAlive();
connection.sendMessage(keepAlive);
```

#### 4. Slow Download Speeds

**Symptoms**: Download speed below expected

**Solutions:**
- Increase MAX_CONNECTIONS for more concurrent peers
- Use RAREST_FIRST strategy if many peers available
- Check if peers are choking

**Example:**
```java
if (connection.isPeerChoking()) {
    log.debug("Peer is choking, sending INTERESTED message");
    connection.sendMessage(PeerMessage.interested());
}
```

### Debug Logging

Enable debug logging to troubleshoot issues:

```java
// In logback.xml
<logger name="com.hypertube.torrent" level="DEBUG"/>
```

**Key log patterns:**
```
// Tracker communication
"Contacting tracker: {url}"
"Received {count} peers from tracker"

// Peer connections
"Connected to peer: {ip}:{port}"
"Handshake successful with peer: {ip}"

// Piece downloads
"Downloading piece {index} of {total}"
"Piece {index} verified successfully"
"Download progress: {progress}%"
```

## Performance Tuning

### Optimal Settings for Video Streaming

```java
// Use sequential strategy
PieceSelectionStrategy.SEQUENTIAL

// Lower buffer threshold for faster start
private static final double BUFFER_THRESHOLD = 5.0;  // 5%

// More concurrent connections
private static final int MAX_CONNECTIONS = 20;
```

### Memory Considerations

- Each `PeerConnection` uses ~100KB memory
- Each `Piece` uses memory equal to piece size (typically 256KB-1MB)
- `DownloadManager` with 10 peers downloading 4 pieces = ~10MB memory

### Network Bandwidth

- Request block size: 16KB
- Typical download: 20 blocks/second/peer = ~320 KB/s per peer
- 10 peers = ~3.2 MB/s total bandwidth

## License

This library is part of the HyperTube project and is licensed under the same terms.

## Contributing

See the main project README for contribution guidelines.

## Support

For issues, questions, or feature requests, please open an issue on the project repository.
