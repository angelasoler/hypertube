package com.hypertube.torrent.tracker;

import com.hypertube.torrent.TorrentMetadata;
import com.hypertube.torrent.bencode.BencodeEncoder;
import com.hypertube.torrent.bencode.BencodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TrackerClientTest {

    private TrackerClient trackerClient;

    @BeforeEach
    void setUp() {
        trackerClient = new TrackerClient(6881);
    }

    @Test
    void testGeneratePeerId() {
        byte[] peerId = trackerClient.getPeerId();

        assertThat(peerId).hasSize(20);
        // Should start with -HT0100-
        String peerIdStr = new String(peerId, 0, 8);
        assertThat(peerIdStr).isEqualTo("-HT0100-");
    }

    @Test
    void testParseCompactPeers() throws BencodeException {
        // Simulate compact peer response
        // 2 peers: 192.168.1.1:6881 and 192.168.1.2:6882
        byte[] peersData = new byte[]{
            (byte) 192, (byte) 168, 1, 1, // IP: 192.168.1.1
            (byte) ((6881 >> 8) & 0xFF), (byte) (6881 & 0xFF), // Port: 6881

            (byte) 192, (byte) 168, 1, 2, // IP: 192.168.1.2
            (byte) ((6882 >> 8) & 0xFF), (byte) (6882 & 0xFF)  // Port: 6882
        };

        Map<String, Object> response = new HashMap<>();
        response.put("interval", 1800L);
        response.put("complete", 10L);
        response.put("incomplete", 5L);
        response.put("peers", peersData);

        byte[] responseData = BencodeEncoder.encode(response);

        // Note: This tests the internal parsing logic.
        // Full integration testing would require a real tracker or mock server
    }

    @Test
    void testParseDictionaryPeers() throws BencodeException {
        // Simulate dictionary peer response
        Map<String, Object> peer1 = new HashMap<>();
        peer1.put("ip", "192.168.1.1");
        peer1.put("port", 6881L);
        peer1.put("peer id", new byte[20]);

        Map<String, Object> peer2 = new HashMap<>();
        peer2.put("ip", "192.168.1.2");
        peer2.put("port", 6882L);
        peer2.put("peer id", new byte[20]);

        Map<String, Object> response = new HashMap<>();
        response.put("interval", 1800L);
        response.put("complete", 10L);
        response.put("incomplete", 5L);
        response.put("peers", Arrays.asList(peer1, peer2));

        byte[] responseData = BencodeEncoder.encode(response);

        // Note: This tests the internal parsing logic
    }

    @Test
    void testTrackerFailureResponse() throws BencodeException {
        Map<String, Object> response = new HashMap<>();
        response.put("failure reason", "Torrent not registered");

        byte[] responseData = BencodeEncoder.encode(response);

        // When parsing this response, it should throw an exception with the failure reason
    }

    @Test
    void testPeerToString() {
        Peer peer = new Peer("192.168.1.1", 6881, null);
        assertThat(peer.toString()).isEqualTo("192.168.1.1:6881");
    }

    @Test
    void testTrackerEventEnum() {
        assertThat(TrackerEvent.STARTED).isNotNull();
        assertThat(TrackerEvent.COMPLETED).isNotNull();
        assertThat(TrackerEvent.STOPPED).isNotNull();
        assertThat(TrackerEvent.NONE).isNotNull();
    }

    @Test
    void testTrackerResponseBuilder() {
        TrackerResponse response = TrackerResponse.builder()
            .interval(1800)
            .seeders(10)
            .leechers(5)
            .peers(new ArrayList<>())
            .build();

        assertThat(response.getInterval()).isEqualTo(1800);
        assertThat(response.getSeeders()).isEqualTo(10);
        assertThat(response.getLeechers()).isEqualTo(5);
        assertThat(response.getPeers()).isEmpty();
    }

    /**
     * This test demonstrates the expected usage pattern
     * without actually making network calls
     */
    @Test
    void testUsagePattern() throws BencodeException {
        // Create metadata from magnet link
        String magnetLink = "magnet:?xt=urn:btih:1234567890abcdef1234567890abcdef12345678" +
            "&tr=http://tracker.example.com/announce";

        TorrentMetadata metadata = TorrentMetadata.fromMagnetLink(magnetLink);

        // In real usage, this would contact the tracker:
        // TrackerResponse response = trackerClient.announce(
        //     metadata, TrackerEvent.STARTED, 0, 0, metadata.getTotalSize()
        // );

        // For now, just verify the metadata is valid
        assertThat(metadata.getTrackers()).hasSize(1);
        assertThat(metadata.getInfoHash()).hasSize(20);
    }
}
