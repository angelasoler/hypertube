package com.hypertube.torrent.manager;

import com.hypertube.torrent.TorrentMetadata;
import com.hypertube.torrent.peer.PeerConnection;
import com.hypertube.torrent.peer.PeerException;
import com.hypertube.torrent.peer.PeerMessage;
import com.hypertube.torrent.piece.Bitfield;
import com.hypertube.torrent.piece.Piece;
import com.hypertube.torrent.tracker.Peer;
import com.hypertube.torrent.tracker.TrackerClient;
import com.hypertube.torrent.tracker.TrackerEvent;
import com.hypertube.torrent.tracker.TrackerResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages torrent downloads across multiple peers
 */
@Slf4j
public class DownloadManager {

    private final TorrentMetadata metadata;
    private final TrackerClient trackerClient;
    private final Path downloadPath;
    private final PieceSelectionStrategy strategy;

    @Getter
    private final Bitfield bitfield;
    private final Map<Integer, Piece> activePieces;
    private final Map<String, PeerConnection> activeConnections;
    private final ExecutorService peerExecutor;
    private final ScheduledExecutorService trackerExecutor;

    @Getter
    private final AtomicLong downloaded;
    @Getter
    private final AtomicLong uploaded;
    private final AtomicBoolean running;

    private RandomAccessFile outputFile;

    public DownloadManager(TorrentMetadata metadata,
                          TrackerClient trackerClient,
                          Path downloadPath,
                          PieceSelectionStrategy strategy) {
        this.metadata = metadata;
        this.trackerClient = trackerClient;
        this.downloadPath = downloadPath;
        this.strategy = strategy;

        this.bitfield = new Bitfield(metadata.getNumPieces());
        this.activePieces = new ConcurrentHashMap<>();
        this.activeConnections = new ConcurrentHashMap<>();
        this.peerExecutor = Executors.newFixedThreadPool(10);
        this.trackerExecutor = Executors.newScheduledThreadPool(1);

        this.downloaded = new AtomicLong(0);
        this.uploaded = new AtomicLong(0);
        this.running = new AtomicBoolean(false);
    }

    /**
     * Start downloading the torrent
     */
    public void start() throws IOException, DownloadException {
        if (running.get()) {
            throw new DownloadException("Download already running");
        }

        log.info("Starting download for: {}", metadata.getName());
        running.set(true);

        // Create output file
        Files.createDirectories(downloadPath.getParent());
        outputFile = new RandomAccessFile(downloadPath.toFile(), "rw");
        outputFile.setLength(metadata.getTotalSize());

        // Contact tracker to get peers
        announceToTracker(TrackerEvent.STARTED);

        // Schedule periodic tracker announces
        trackerExecutor.scheduleAtFixedRate(
            () -> announceToTracker(TrackerEvent.NONE),
            30, 30, TimeUnit.MINUTES
        );
    }

    /**
     * Stop downloading
     */
    public void stop() {
        if (!running.get()) {
            return;
        }

        log.info("Stopping download for: {}", metadata.getName());
        running.set(false);

        // Disconnect all peers
        activeConnections.values().forEach(conn -> {
            try {
                conn.close();
            } catch (Exception e) {
                log.warn("Error closing peer connection", e);
            }
        });
        activeConnections.clear();

        // Shutdown executors
        peerExecutor.shutdown();
        trackerExecutor.shutdown();

        // Close output file
        if (outputFile != null) {
            try {
                outputFile.close();
            } catch (IOException e) {
                log.warn("Error closing output file", e);
            }
        }

        // Final tracker announce
        announceToTracker(TrackerEvent.STOPPED);
    }

    /**
     * Check if download is complete
     */
    public boolean isComplete() {
        return bitfield.isComplete();
    }

    /**
     * Get download progress percentage
     */
    public double getProgress() {
        return bitfield.getCompletionPercentage();
    }

    /**
     * Get download speed in bytes/sec
     */
    public long getDownloadSpeed() {
        // TODO: Implement speed calculation
        return 0;
    }

    /**
     * Add a peer to download from
     */
    public void addPeer(Peer peer) {
        String peerId = peer.getIp() + ":" + peer.getPort();

        if (activeConnections.containsKey(peerId)) {
            log.debug("Already connected to peer: {}", peerId);
            return;
        }

        if (activeConnections.size() >= 10) {
            log.debug("Max peer connections reached, ignoring peer: {}", peerId);
            return;
        }

        peerExecutor.submit(() -> connectToPeer(peer));
    }

    /**
     * Get number of active peer connections
     */
    public int getActivePeerCount() {
        return activeConnections.size();
    }

    /**
     * Connect to a peer and start downloading
     */
    private void connectToPeer(Peer peer) {
        String peerId = peer.getIp() + ":" + peer.getPort();

        try {
            log.info("Connecting to peer: {}", peerId);

            PeerConnection connection = new PeerConnection(
                peer.getIp(),
                peer.getPort(),
                metadata.getInfoHash(),
                trackerClient.getPeerId()
            );

            connection.connect();
            activeConnections.put(peerId, connection);

            // Express interest
            connection.sendInterested();

            // Download from this peer
            downloadFromPeer(connection);

        } catch (PeerException e) {
            log.warn("Failed to connect to peer {}: {}", peerId, e.getMessage());
            activeConnections.remove(peerId);
        }
    }

    /**
     * Download pieces from a peer
     */
    private void downloadFromPeer(PeerConnection connection) {
        while (running.get() && !isComplete()) {
            try {
                // Wait for unchoke
                PeerMessage msg = connection.receiveMessage();

                if (msg.getType() == PeerMessage.MessageType.UNCHOKE) {
                    log.debug("Unchoked by peer");

                    // Download pieces while unchoked
                    while (running.get() && !isComplete()) {
                        // Select next piece to download
                        int pieceIndex = selectNextPiece(connection);
                        if (pieceIndex == -1) {
                            log.debug("No pieces to download from this peer");
                            break;
                        }

                        // Download the piece
                        downloadPiece(connection, pieceIndex);
                    }

                } else if (msg.getType() == PeerMessage.MessageType.CHOKE) {
                    log.debug("Choked by peer");

                } else if (msg.getType() == PeerMessage.MessageType.HAVE) {
                    int pieceIndex = msg.getHavePieceIndex();
                    log.debug("Peer has piece: {}", pieceIndex);
                }

            } catch (PeerException e) {
                log.warn("Error downloading from peer: {}", e.getMessage());
                break;
            }
        }
    }

    /**
     * Select next piece to download using the configured strategy
     */
    private int selectNextPiece(PeerConnection connection) {
        switch (strategy) {
            case SEQUENTIAL:
                return bitfield.getNextMissingPiece();

            case RAREST_FIRST:
                // For now, just use sequential
                // TODO: Implement rarest-first using peer bitfields
                return bitfield.getNextMissingPiece();

            default:
                return -1;
        }
    }

    /**
     * Download a single piece from a peer
     */
    private void downloadPiece(PeerConnection connection, int pieceIndex) throws PeerException {
        // Get piece hash from metadata
        byte[] pieceHash = metadata.getPieceHash(pieceIndex);
        int pieceLength = metadata.getPieceLength(pieceIndex);

        // Create piece object
        Piece piece = new Piece(pieceIndex, pieceLength, pieceHash);
        activePieces.put(pieceIndex, piece);

        log.debug("Downloading piece {}/{}", pieceIndex + 1, metadata.getNumPieces());

        try {
            // Download all blocks in the piece
            while (!piece.isComplete()) {
                Piece.BlockRequest request = piece.getNextBlockRequest();
                if (request == null) break;

                // Request block from peer
                connection.requestBlock(
                    request.getPieceIndex(),
                    request.getBegin(),
                    request.getLength()
                );

                // Wait for piece message
                PeerMessage msg = connection.receiveMessage();
                if (msg.getType() != PeerMessage.MessageType.PIECE) {
                    log.warn("Expected PIECE message, got: {}", msg.getType());
                    continue;
                }

                // Write block to piece
                PeerMessage.BlockInfo block = msg.getBlockInfo();
                piece.writeBlock(block.getBegin(), block.getData());

                // Update downloaded counter
                downloaded.addAndGet(block.getData().length);
            }

            // Verify piece
            if (piece.verify()) {
                log.info("Piece {} verified successfully", pieceIndex);

                // Write to disk
                writePieceToDisk(pieceIndex, piece.getData());

                // Update bitfield
                bitfield.setPiece(pieceIndex);

                // Notify tracker if complete
                if (isComplete()) {
                    announceToTracker(TrackerEvent.COMPLETED);
                }

            } else {
                log.warn("Piece {} verification failed, will retry", pieceIndex);
                piece.reset();
            }

        } finally {
            activePieces.remove(pieceIndex);
        }
    }

    /**
     * Write piece data to disk
     */
    private synchronized void writePieceToDisk(int pieceIndex, byte[] data) {
        try {
            long offset = (long) pieceIndex * metadata.getPieceLength();
            outputFile.seek(offset);
            outputFile.write(data);

            log.debug("Wrote piece {} to disk at offset {}", pieceIndex, offset);

        } catch (IOException e) {
            log.error("Failed to write piece {} to disk", pieceIndex, e);
        }
    }

    /**
     * Announce to tracker
     */
    private void announceToTracker(TrackerEvent event) {
        try {
            long left = metadata.getTotalSize() - (bitfield.getPiecesCount() * metadata.getPieceLength());

            TrackerResponse response = trackerClient.announce(
                metadata,
                event,
                downloaded.get(),
                uploaded.get(),
                left
            );

            log.info("Tracker response: {} seeders, {} leechers, {} peers",
                response.getSeeders(), response.getLeechers(), response.getPeers().size());

            // Add new peers
            if (running.get()) {
                response.getPeers().forEach(this::addPeer);
            }

        } catch (Exception e) {
            log.warn("Failed to announce to tracker: {}", e.getMessage());
        }
    }
}
