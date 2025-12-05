package com.hypertube.torrent;

import com.hypertube.torrent.bencode.BencodeEncoder;
import com.hypertube.torrent.bencode.BencodeException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TorrentMetadataTest {

    @Test
    void testParseSingleFileTorrent() throws BencodeException {
        // Create a minimal valid .torrent file structure
        Map<String, Object> torrent = new HashMap<>();
        torrent.put("announce", "http://tracker.example.com:8080/announce");

        Map<String, Object> info = new HashMap<>();
        info.put("name", "example.mkv");
        info.put("piece length", 262144L);
        info.put("length", 1048576L); // 1 MB
        info.put("pieces", new byte[20]); // Single piece hash

        torrent.put("info", info);

        byte[] torrentData = BencodeEncoder.encode(torrent);
        TorrentMetadata metadata = TorrentMetadata.fromTorrentFile(torrentData);

        assertThat(metadata.getName()).isEqualTo("example.mkv");
        assertThat(metadata.getTotalSize()).isEqualTo(1048576L);
        assertThat(metadata.getPieceLength()).isEqualTo(262144L);
        assertThat(metadata.getNumPieces()).isEqualTo(1);
        assertThat(metadata.getFiles()).hasSize(1);
        assertThat(metadata.getFiles().get(0).getPath()).isEqualTo("example.mkv");
        assertThat(metadata.getFiles().get(0).getLength()).isEqualTo(1048576L);
        assertThat(metadata.getTrackers()).contains("http://tracker.example.com:8080/announce");
        assertThat(metadata.getInfoHash()).isNotNull();
        assertThat(metadata.getInfoHash()).hasSize(20); // SHA-1 is 20 bytes
    }

    @Test
    void testParseMultiFileTorrent() throws BencodeException {
        Map<String, Object> torrent = new HashMap<>();
        torrent.put("announce", "http://tracker.example.com:8080/announce");

        Map<String, Object> info = new HashMap<>();
        info.put("name", "movie");
        info.put("piece length", 262144L);
        info.put("pieces", new byte[40]); // 2 piece hashes

        // Multiple files
        Map<String, Object> file1 = new HashMap<>();
        file1.put("length", 524288L);
        file1.put("path", Arrays.asList("video.mkv"));

        Map<String, Object> file2 = new HashMap<>();
        file2.put("length", 1024L);
        file2.put("path", Arrays.asList("subtitle.srt"));

        info.put("files", Arrays.asList(file1, file2));

        torrent.put("info", info);

        byte[] torrentData = BencodeEncoder.encode(torrent);
        TorrentMetadata metadata = TorrentMetadata.fromTorrentFile(torrentData);

        assertThat(metadata.getName()).isEqualTo("movie");
        assertThat(metadata.getTotalSize()).isEqualTo(525312L); // 524288 + 1024
        assertThat(metadata.getNumPieces()).isEqualTo(2);
        assertThat(metadata.getFiles()).hasSize(2);
        assertThat(metadata.getFiles().get(0).getPath()).isEqualTo("movie/video.mkv");
        assertThat(metadata.getFiles().get(1).getPath()).isEqualTo("movie/subtitle.srt");
    }

    @Test
    void testParseMultiTrackerTorrent() throws BencodeException {
        Map<String, Object> torrent = new HashMap<>();
        torrent.put("announce", "http://tracker1.example.com/announce");

        // Multi-tracker extension
        torrent.put("announce-list", Arrays.asList(
            Arrays.asList("http://tracker1.example.com/announce"),
            Arrays.asList("http://tracker2.example.com/announce", "http://tracker3.example.com/announce")
        ));

        Map<String, Object> info = new HashMap<>();
        info.put("name", "example.mkv");
        info.put("piece length", 262144L);
        info.put("length", 1048576L);
        info.put("pieces", new byte[20]);

        torrent.put("info", info);

        byte[] torrentData = BencodeEncoder.encode(torrent);
        TorrentMetadata metadata = TorrentMetadata.fromTorrentFile(torrentData);

        assertThat(metadata.getTrackers()).hasSize(3);
        assertThat(metadata.getTrackers()).containsExactlyInAnyOrder(
            "http://tracker1.example.com/announce",
            "http://tracker2.example.com/announce",
            "http://tracker3.example.com/announce"
        );
    }

    @Test
    void testParseMagnetLink() throws BencodeException {
        String magnetLink = "magnet:?xt=urn:btih:1234567890abcdef1234567890abcdef12345678" +
            "&dn=Example+Movie" +
            "&tr=http://tracker1.example.com/announce" +
            "&tr=http://tracker2.example.com/announce";

        TorrentMetadata metadata = TorrentMetadata.fromMagnetLink(magnetLink);

        assertThat(metadata.getName()).isEqualTo("Example Movie");
        assertThat(metadata.getInfoHash()).isNotNull();
        assertThat(metadata.getInfoHash()).hasSize(20);
        assertThat(metadata.getTrackers()).hasSize(2);
        assertThat(metadata.getTrackers()).containsExactlyInAnyOrder(
            "http://tracker1.example.com/announce",
            "http://tracker2.example.com/announce"
        );
    }

    @Test
    void testParseMagnetLinkWithoutName() throws BencodeException {
        String magnetLink = "magnet:?xt=urn:btih:1234567890abcdef1234567890abcdef12345678";

        TorrentMetadata metadata = TorrentMetadata.fromMagnetLink(magnetLink);

        assertThat(metadata.getName()).isEqualTo("Unknown");
        assertThat(metadata.getInfoHash()).isNotNull();
    }

    @Test
    void testGetInfoHashHex() throws BencodeException {
        String magnetLink = "magnet:?xt=urn:btih:1234567890abcdef1234567890abcdef12345678";
        TorrentMetadata metadata = TorrentMetadata.fromMagnetLink(magnetLink);

        String hex = metadata.getInfoHashHex();
        assertThat(hex).hasSize(40); // 20 bytes = 40 hex chars
        assertThat(hex).isEqualTo("1234567890abcdef1234567890abcdef12345678");
    }

    @Test
    void testGetInfoHashUrlEncoded() throws BencodeException {
        String magnetLink = "magnet:?xt=urn:btih:1234567890abcdef1234567890abcdef12345678";
        TorrentMetadata metadata = TorrentMetadata.fromMagnetLink(magnetLink);

        String urlEncoded = metadata.getInfoHashUrlEncoded();
        assertThat(urlEncoded).isNotNull();
        assertThat(urlEncoded).contains("%"); // Should have percent-encoded bytes
    }

    @Test
    void testMissingInfoDictionary() {
        Map<String, Object> torrent = new HashMap<>();
        torrent.put("announce", "http://tracker.example.com/announce");
        // Missing 'info' dictionary

        assertThatThrownBy(() -> {
            byte[] torrentData = BencodeEncoder.encode(torrent);
            TorrentMetadata.fromTorrentFile(torrentData);
        }).isInstanceOf(BencodeException.class)
          .hasMessageContaining("Missing 'info' dictionary");
    }

    @Test
    void testMissingRequiredFields() {
        Map<String, Object> torrent = new HashMap<>();
        torrent.put("announce", "http://tracker.example.com/announce");

        Map<String, Object> info = new HashMap<>();
        info.put("name", "example.mkv");
        // Missing 'piece length' and 'pieces'

        torrent.put("info", info);

        assertThatThrownBy(() -> {
            byte[] torrentData = BencodeEncoder.encode(torrent);
            TorrentMetadata.fromTorrentFile(torrentData);
        }).isInstanceOf(BencodeException.class)
          .hasMessageContaining("Missing required fields");
    }

    @Test
    void testInvalidMagnetLink() {
        String invalidMagnet = "http://not-a-magnet-link.com";

        assertThatThrownBy(() -> TorrentMetadata.fromMagnetLink(invalidMagnet))
            .isInstanceOf(BencodeException.class)
            .hasMessageContaining("Invalid magnet link format");
    }

    @Test
    void testMagnetLinkWithoutInfoHash() {
        String magnetLink = "magnet:?dn=Example+Movie&tr=http://tracker.example.com/announce";

        assertThatThrownBy(() -> TorrentMetadata.fromMagnetLink(magnetLink))
            .isInstanceOf(BencodeException.class)
            .hasMessageContaining("Magnet link missing info hash");
    }

    @Test
    void testInfoHashConsistency() throws BencodeException {
        // Same info dictionary should produce same info hash
        Map<String, Object> info = new HashMap<>();
        info.put("name", "test.mkv");
        info.put("piece length", 262144L);
        info.put("length", 1048576L);
        info.put("pieces", new byte[20]);

        Map<String, Object> torrent1 = new HashMap<>();
        torrent1.put("announce", "http://tracker1.example.com/announce");
        torrent1.put("info", info);

        Map<String, Object> torrent2 = new HashMap<>();
        torrent2.put("announce", "http://tracker2.example.com/announce");
        torrent2.put("info", info);

        byte[] torrentData1 = BencodeEncoder.encode(torrent1);
        byte[] torrentData2 = BencodeEncoder.encode(torrent2);

        TorrentMetadata metadata1 = TorrentMetadata.fromTorrentFile(torrentData1);
        TorrentMetadata metadata2 = TorrentMetadata.fromTorrentFile(torrentData2);

        // Info hashes should be identical despite different announce URLs
        assertThat(metadata1.getInfoHash()).isEqualTo(metadata2.getInfoHash());
    }
}
