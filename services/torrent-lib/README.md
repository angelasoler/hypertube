# HyperTube Torrent Library

A pure Java implementation of the BitTorrent protocol for video streaming.

## Features

### ✅ Implemented
- **Bencode Codec**: Complete encoder/decoder for BitTorrent's bencode format
- **Torrent Metadata Parser**: Parse .torrent files and magnet links
- **Tracker Client**: HTTP/HTTPS tracker communication with peer discovery
- **Info Hash Calculation**: SHA-1 based info hash for torrent identification
- **Multi-tracker Support**: Handle announce-list for redundancy
- **Compact & Dictionary Peer Format**: Support both peer list formats
- **Peer Wire Protocol**: Complete message protocol implementation
- **Peer Connection**: Handshake and bidirectional communication
- **Piece Management**: Block-based downloading with SHA-1 verification
- **Bitfield**: Piece availability tracking
- **Download Manager**: Multi-peer download orchestration with sequential/rarest-first strategies
- **Piece Selection Strategies**: Sequential (for streaming) and rarest-first modes

### 🚧 To Be Implemented
- **DHT Support**: Distributed Hash Table for trackerless torrents
- **UDP Tracker Support**: Faster tracker protocol
- **PEX (Peer Exchange)**: Peer discovery without trackers
- **Upload/Seeding**: Upload blocks to other peers
- **Choking Algorithm**: Optimistic unchoking and peer management

## Architecture

```
torrent-lib/
├── bencode/          # Bencode encoding/decoding
│   ├── BencodeDecoder
│   ├── BencodeEncoder
│   └── BencodeException
├── tracker/          # Tracker communication
│   ├── TrackerClient
│   ├── TrackerResponse
│   ├── TrackerEvent
│   ├── Peer
│   └── TrackerException
├── peer/             # Peer wire protocol
│   ├── PeerMessage   (Message encoding/decoding)
│   ├── PeerConnection (Handshake & communication)
│   └── PeerException
├── piece/            # Piece & block management
│   ├── Piece         (Block-based piece with verification)
│   └── Bitfield      (Piece availability tracking)
├── manager/          # Download orchestration
│   ├── DownloadManager       (Multi-peer coordination)
│   ├── PieceSelectionStrategy (Sequential/rarest-first)
│   └── DownloadException
└── TorrentMetadata  # Torrent file/magnet parser
```

## Usage

### Parse Torrent File

```java
// From .torrent file
byte[] torrentData = Files.readAllBytes(Path.of("movie.torrent"));
TorrentMetadata metadata = TorrentMetadata.fromTorrentFile(torrentData);

System.out.println("Name: " + metadata.getName());
System.out.println("Size: " + metadata.getTotalSize());
System.out.println("Pieces: " + metadata.getNumPieces());
System.out.println("Info Hash: " + metadata.getInfoHashHex());
```

### Parse Magnet Link

```java
String magnetLink = "magnet:?xt=urn:btih:..." +
    "&dn=Movie+Name" +
    "&tr=http://tracker.example.com/announce";

TorrentMetadata metadata = TorrentMetadata.fromMagnetLink(magnetLink);
```

### Contact Tracker

```java
TrackerClient tracker = new TrackerClient(6881); // listening port

TrackerResponse response = tracker.announce(
    metadata,
    TrackerEvent.STARTED,
    0,      // downloaded
    0,      // uploaded
    metadata.getTotalSize() // left to download
);

System.out.println("Seeders: " + response.getSeeders());
System.out.println("Leechers: " + response.getLeechers());
System.out.println("Peers: " + response.getPeers().size());

for (Peer peer : response.getPeers()) {
    System.out.println("  - " + peer.getIp() + ":" + peer.getPort());
}
```

### Connect to Peer and Download

```java
// Get peers from tracker
TrackerClient tracker = new TrackerClient(6881);
TrackerResponse response = tracker.announce(metadata, TrackerEvent.STARTED, 0, 0, totalSize);

// Connect to first peer
Peer peer = response.getPeers().get(0);
PeerConnection connection = new PeerConnection(
    peer.getIp(),
    peer.getPort(),
    metadata.getInfoHash(),
    tracker.getPeerId()
);

connection.connect(); // Performs handshake

// Express interest
connection.sendInterested();

// Wait for unchoke
PeerMessage msg = connection.receiveMessage();
if (msg.getType() == PeerMessage.MessageType.UNCHOKE) {
    // Request first block of first piece
    connection.requestBlock(0, 0, 16384);

    // Receive piece
    msg = connection.receiveMessage();
    if (msg.getType() == PeerMessage.MessageType.PIECE) {
        PeerMessage.BlockInfo block = msg.getBlockInfo();
        System.out.println("Downloaded block from piece " + block.getPieceIndex());
    }
}

connection.close();
```

### Manage Pieces

```java
// Create piece with expected hash
byte[] expectedHash = ...; // From torrent metadata
Piece piece = new Piece(0, 262144, expectedHash);

// Download blocks
while (!piece.isComplete()) {
    Piece.BlockRequest request = piece.getNextBlockRequest();
    if (request == null) break;

    // Request block from peer
    connection.requestBlock(
        request.getPieceIndex(),
        request.getBegin(),
        request.getLength()
    );

    // Receive and write block
    PeerMessage msg = connection.receiveMessage();
    PeerMessage.BlockInfo block = msg.getBlockInfo();
    piece.writeBlock(block.getBegin(), block.getData());
}

// Verify integrity
if (piece.verify()) {
    byte[] pieceData = piece.getData();
    System.out.println("Piece verified and complete!");
}
```

### Track Download Progress

```java
Bitfield bitfield = new Bitfield(metadata.getNumPieces());

// Mark pieces as downloaded
bitfield.setPiece(0);
bitfield.setPiece(1);

System.out.println(bitfield); // Bitfield[2/100 pieces (2.0%)]

// Find next piece to download
int nextPiece = bitfield.getNextMissingPiece();

// Check if download is complete
if (bitfield.isComplete()) {
    System.out.println("Download complete!");
}
```

### Bencode Encoding/Decoding

```java
// Encode
Map<String, Object> data = new HashMap<>();
data.put("name", "example");
data.put("size", 1024L);

byte[] encoded = BencodeEncoder.encode(data);

// Decode
Object decoded = BencodeDecoder.decode(encoded);
Map<String, Object> map = (Map<String, Object>) decoded;

String name = BencodeDecoder.getString(map, "name");
Long size = BencodeDecoder.getLong(map, "size");
```

### Complete Download with Download Manager

```java
// Parse torrent file
byte[] torrentData = Files.readAllBytes(Path.of("movie.torrent"));
TorrentMetadata metadata = TorrentMetadata.fromTorrentFile(torrentData);

// Create tracker client and download manager
TrackerClient trackerClient = new TrackerClient(6881);
Path downloadPath = Path.of("/downloads/movie.mkv");

DownloadManager manager = new DownloadManager(
    metadata,
    trackerClient,
    downloadPath,
    PieceSelectionStrategy.SEQUENTIAL // Best for video streaming
);

// Start download
manager.start();

// Monitor progress
while (!manager.isComplete()) {
    System.out.printf("Progress: %.2f%% - %d peers - %d KB/s%n",
        manager.getProgress(),
        manager.getActivePeerCount(),
        manager.getDownloadSpeed() / 1024
    );
    Thread.sleep(1000);
}

// Stop when complete
manager.stop();
System.out.println("Download complete!");
```

## Testing

Run all unit tests:

```bash
cd services/torrent-lib
mvn test
```

Current test coverage:
- ✅ Bencode encoder/decoder (30+ test cases)
- ✅ Torrent metadata parser (20+ test cases)
- ✅ Tracker client (10+ test cases)

## Integration with HyperTube

This library can be integrated into the video-streaming service to replace the libtorrent4j dependency:

```java
// In video-streaming service
@Service
public class TorrentDownloadService {
    private final TrackerClient trackerClient;

    public void startDownload(String magnetLink) {
        TorrentMetadata metadata = TorrentMetadata.fromMagnetLink(magnetLink);
        TrackerResponse response = trackerClient.announce(
            metadata, TrackerEvent.STARTED, 0, 0, metadata.getTotalSize()
        );

        // Connect to peers and start downloading
        // (Peer protocol implementation needed)
    }
}
```

## Dependencies

- Java 17
- SLF4J (logging)
- Lombok (boilerplate reduction)
- JUnit 5 (testing)
- AssertJ (test assertions)

## Protocol References

- [BEP 3: BitTorrent Protocol Specification](http://bittorrent.org/beps/bep_0003.html)
- [BEP 12: Multitracker Metadata Extension](http://bittorrent.org/beps/bep_0012.html)
- [BEP 23: Tracker Returns Compact Peer Lists](http://bittorrent.org/beps/bep_0023.html)
- [Bencode Specification](https://wiki.theory.org/BitTorrentSpecification#Bencoding)

## License

Part of the HyperTube project.
