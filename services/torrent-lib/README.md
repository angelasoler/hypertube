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

### 🚧 To Be Implemented
- **Peer Wire Protocol**: Direct peer-to-peer communication
- **Piece Manager**: Download orchestration and verification
- **DHT Support**: Distributed Hash Table for trackerless torrents
- **UDP Tracker Support**: Faster tracker protocol
- **PEX (Peer Exchange)**: Peer discovery without trackers

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
├── TorrentMetadata  # Torrent file/magnet parser
├── peer/            # [TODO] Peer protocol
├── piece/           # [TODO] Piece management
└── manager/         # [TODO] Download orchestration
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
