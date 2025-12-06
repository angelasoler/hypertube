package com.hypertube.torrent.manager;

import com.hypertube.torrent.TorrentMetadata;
import com.hypertube.torrent.bencode.BencodeEncoder;
import com.hypertube.torrent.bencode.BencodeException;
import com.hypertube.torrent.tracker.Peer;
import com.hypertube.torrent.tracker.TrackerClient;
import com.hypertube.torrent.tracker.TrackerEvent;
import com.hypertube.torrent.tracker.TrackerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DownloadManagerTest {

    @TempDir
    Path tempDir;

    private TorrentMetadata metadata;
    private TrackerClient trackerClient;
    private Path downloadPath;

    @BeforeEach
    void setUp() throws BencodeException, IOException {
        // Create test torrent metadata
        metadata = createTestMetadata();

        // Mock tracker client
        trackerClient = mock(TrackerClient.class);
        when(trackerClient.getPeerId()).thenReturn(new byte[20]);

        // Create download path
        downloadPath = tempDir.resolve("test.dat");
    }

    @AfterEach
    void tearDown() {
        // Cleanup
        if (downloadPath != null && Files.exists(downloadPath)) {
            try {
                Files.delete(downloadPath);
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    @Test
    void testCreateDownloadManager() {
        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        assertThat(manager).isNotNull();
        assertThat(manager.getBitfield()).isNotNull();
        assertThat(manager.getBitfield().getTotalPieces()).isEqualTo(metadata.getNumPieces());
        assertThat(manager.isComplete()).isFalse();
        assertThat(manager.getProgress()).isEqualTo(0.0);
    }

    @Test
    void testStartDownload() throws Exception {
        // Mock tracker response with no peers
        TrackerResponse response = TrackerResponse.builder()
            .interval(1800)
            .seeders(0)
            .leechers(0)
            .peers(Collections.emptyList())
            .build();

        when(trackerClient.announce(
            eq(metadata),
            eq(TrackerEvent.STARTED),
            anyLong(),
            anyLong(),
            anyLong()
        )).thenReturn(response);

        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        manager.start();

        // Verify file was created
        assertThat(Files.exists(downloadPath)).isTrue();
        assertThat(Files.size(downloadPath)).isEqualTo(metadata.getTotalSize());

        // Verify tracker was contacted
        verify(trackerClient).announce(
            eq(metadata),
            eq(TrackerEvent.STARTED),
            anyLong(),
            anyLong(),
            anyLong()
        );

        manager.stop();
    }

    @Test
    void testStartDownloadTwice() throws Exception {
        TrackerResponse response = TrackerResponse.builder()
            .interval(1800)
            .seeders(0)
            .leechers(0)
            .peers(Collections.emptyList())
            .build();

        when(trackerClient.announce(any(), any(), anyLong(), anyLong(), anyLong()))
            .thenReturn(response);

        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        manager.start();

        // Starting again should throw exception
        assertThatThrownBy(() -> manager.start())
            .isInstanceOf(DownloadException.class)
            .hasMessageContaining("already running");

        manager.stop();
    }

    @Test
    void testStopDownload() throws Exception {
        TrackerResponse response = TrackerResponse.builder()
            .interval(1800)
            .seeders(0)
            .leechers(0)
            .peers(Collections.emptyList())
            .build();

        when(trackerClient.announce(any(), any(), anyLong(), anyLong(), anyLong()))
            .thenReturn(response);

        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        manager.start();
        manager.stop();

        // Verify STOPPED event was sent to tracker
        verify(trackerClient).announce(
            eq(metadata),
            eq(TrackerEvent.STOPPED),
            anyLong(),
            anyLong(),
            anyLong()
        );
    }

    @Test
    void testAddPeer() throws Exception {
        TrackerResponse response = TrackerResponse.builder()
            .interval(1800)
            .seeders(0)
            .leechers(0)
            .peers(Collections.emptyList())
            .build();

        when(trackerClient.announce(any(), any(), anyLong(), anyLong(), anyLong()))
            .thenReturn(response);

        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        manager.start();

        // Add peer
        Peer peer = new Peer("192.168.1.1", 6881, null);
        manager.addPeer(peer);

        // Give it a moment to attempt connection
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }

        manager.stop();
    }

    @Test
    void testGetProgress() {
        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        assertThat(manager.getProgress()).isEqualTo(0.0);

        // Simulate downloading some pieces
        manager.getBitfield().setPiece(0);
        assertThat(manager.getProgress()).isGreaterThan(0.0);

        manager.getBitfield().setPiece(1);
        assertThat(manager.getProgress()).isGreaterThan(0.0);
    }

    @Test
    void testIsComplete() {
        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        assertThat(manager.isComplete()).isFalse();

        // Download all pieces
        for (int i = 0; i < metadata.getNumPieces(); i++) {
            manager.getBitfield().setPiece(i);
        }

        assertThat(manager.isComplete()).isTrue();
    }

    @Test
    void testSequentialStrategy() {
        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        // Sequential should download pieces in order
        assertThat(manager.getBitfield().getNextMissingPiece()).isEqualTo(0);

        manager.getBitfield().setPiece(0);
        assertThat(manager.getBitfield().getNextMissingPiece()).isEqualTo(1);

        manager.getBitfield().setPiece(1);
        assertThat(manager.getBitfield().getNextMissingPiece()).isEqualTo(2);
    }

    @Test
    void testRarestFirstStrategy() {
        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.RAREST_FIRST
        );

        // For now, rarest first falls back to sequential
        // TODO: Implement proper rarest-first when we have peer bitfield tracking
        assertThat(manager.getBitfield().getNextMissingPiece()).isEqualTo(0);
    }

    @Test
    void testDownloadedCounter() {
        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        assertThat(manager.getDownloaded().get()).isEqualTo(0);

        // Simulate downloading
        manager.getDownloaded().addAndGet(16384);
        assertThat(manager.getDownloaded().get()).isEqualTo(16384);
    }

    @Test
    void testUploadedCounter() {
        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        assertThat(manager.getUploaded().get()).isEqualTo(0);

        // Simulate uploading
        manager.getUploaded().addAndGet(16384);
        assertThat(manager.getUploaded().get()).isEqualTo(16384);
    }

    @Test
    void testActivePeerCount() {
        DownloadManager manager = new DownloadManager(
            metadata,
            trackerClient,
            downloadPath,
            PieceSelectionStrategy.SEQUENTIAL
        );

        assertThat(manager.getActivePeerCount()).isEqualTo(0);
    }

    @Test
    void testGetPieceHash() {
        byte[] hash = metadata.getPieceHash(0);
        assertThat(hash).hasSize(20);
    }

    @Test
    void testGetPieceLength() {
        // First piece should be full length
        int length0 = metadata.getPieceLength(0);
        assertThat(length0).isEqualTo((int) metadata.getPieceLength());

        // Last piece may be shorter
        int lastPieceIndex = metadata.getNumPieces() - 1;
        int lastLength = metadata.getPieceLength(lastPieceIndex);
        assertThat(lastLength).isGreaterThan(0);
        assertThat(lastLength).isLessThanOrEqualTo((int) metadata.getPieceLength());
    }

    /**
     * Create test torrent metadata for testing
     */
    private TorrentMetadata createTestMetadata() throws BencodeException {
        Map<String, Object> torrent = new HashMap<>();
        torrent.put("announce", "http://tracker.example.com:8080/announce");

        Map<String, Object> info = new HashMap<>();
        info.put("name", "test.dat");
        info.put("piece length", 16384L); // 16 KB pieces
        info.put("length", 49152L); // 48 KB total = 3 pieces

        // Create piece hashes (3 pieces * 20 bytes each)
        byte[] pieces = new byte[60];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            for (int i = 0; i < 3; i++) {
                byte[] hash = digest.digest(new byte[16384]);
                System.arraycopy(hash, 0, pieces, i * 20, 20);
            }
        } catch (Exception e) {
            throw new BencodeException("Failed to create test hashes", e);
        }
        info.put("pieces", pieces);

        torrent.put("info", info);

        byte[] torrentData = BencodeEncoder.encode(torrent);
        return TorrentMetadata.fromTorrentFile(torrentData);
    }
}
