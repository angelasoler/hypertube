package com.hypertube.torrent.peer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Connection to a BitTorrent peer
 * Handles handshake and message exchange
 */
@Slf4j
public class PeerConnection {
    private final String host;
    private final int port;
    private final byte[] infoHash;
    private final byte[] peerId;

    private Socket socket;
    private InputStream input;
    private OutputStream output;

    @Getter
    private byte[] remotePeerId;

    @Getter
    private BitSet peerPieces;

    @Getter
    private boolean amChoking = true;
    @Getter
    private boolean amInterested = false;
    @Getter
    private boolean peerChoking = true;
    @Getter
    private boolean peerInterested = false;

    private final BlockingQueue<PeerMessage> incomingMessages = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    private static final int HANDSHAKE_SIZE = 68;
    private static final byte[] PROTOCOL_ID = "BitTorrent protocol".getBytes();
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 60000;    // 60 seconds

    public PeerConnection(String host, int port, byte[] infoHash, byte[] peerId) {
        this.host = host;
        this.port = port;
        this.infoHash = infoHash;
        this.peerId = peerId;
    }

    /**
     * Connect to peer and perform handshake
     */
    public void connect() throws PeerException {
        try {
            log.debug("Connecting to peer {}:{}", host, port);
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);

            input = socket.getInputStream();
            output = socket.getOutputStream();

            performHandshake();
            running = true;

            log.info("Connected to peer {}:{}", host, port);
        } catch (IOException e) {
            throw new PeerException("Failed to connect to peer: " + e.getMessage(), e);
        }
    }

    /**
     * Perform BitTorrent handshake
     *
     * Handshake format:
     * <pstrlen><pstr><reserved><info_hash><peer_id>
     * - pstrlen: 1 byte (19)
     * - pstr: 19 bytes ("BitTorrent protocol")
     * - reserved: 8 bytes (all zeros)
     * - info_hash: 20 bytes
     * - peer_id: 20 bytes
     */
    private void performHandshake() throws PeerException, IOException {
        // Send handshake
        ByteBuffer handshake = ByteBuffer.allocate(HANDSHAKE_SIZE);
        handshake.put((byte) 19);              // pstrlen
        handshake.put(PROTOCOL_ID);            // pstr
        handshake.put(new byte[8]);            // reserved
        handshake.put(infoHash);               // info_hash
        handshake.put(peerId);                 // peer_id

        output.write(handshake.array());
        output.flush();

        // Receive handshake
        byte[] response = new byte[HANDSHAKE_SIZE];
        int totalRead = 0;
        while (totalRead < HANDSHAKE_SIZE) {
            int read = input.read(response, totalRead, HANDSHAKE_SIZE - totalRead);
            if (read == -1) {
                throw new PeerException("Peer closed connection during handshake");
            }
            totalRead += read;
        }

        // Validate handshake
        if (response[0] != 19) {
            throw new PeerException("Invalid handshake: wrong protocol string length");
        }

        byte[] pstr = Arrays.copyOfRange(response, 1, 20);
        if (!Arrays.equals(pstr, PROTOCOL_ID)) {
            throw new PeerException("Invalid handshake: wrong protocol string");
        }

        byte[] receivedInfoHash = Arrays.copyOfRange(response, 28, 48);
        if (!Arrays.equals(receivedInfoHash, infoHash)) {
            throw new PeerException("Invalid handshake: info hash mismatch");
        }

        remotePeerId = Arrays.copyOfRange(response, 48, 68);
        log.debug("Handshake successful with peer {}", new String(remotePeerId));
    }

    /**
     * Send a message to peer
     */
    public synchronized void sendMessage(PeerMessage message) throws PeerException {
        if (!running) {
            throw new PeerException("Connection not active");
        }

        try {
            byte[] encoded = message.encode();
            output.write(encoded);
            output.flush();
            log.trace("Sent message: {}", message);

            // Update state based on message
            updateStateFromSentMessage(message);
        } catch (IOException e) {
            throw new PeerException("Failed to send message: " + e.getMessage(), e);
        }
    }

    /**
     * Receive a message from peer (blocking)
     */
    public PeerMessage receiveMessage() throws PeerException {
        if (!running) {
            throw new PeerException("Connection not active");
        }

        try {
            // Read message length (4 bytes)
            byte[] lengthBytes = new byte[4];
            int read = input.read(lengthBytes);
            if (read == -1) {
                throw new PeerException("Peer closed connection");
            }

            int length = ByteBuffer.wrap(lengthBytes).getInt();

            // Keep-alive message
            if (length == 0) {
                log.trace("Received keep-alive");
                return new PeerMessage(PeerMessage.MessageType.KEEP_ALIVE);
            }

            // Read message ID and payload
            byte[] messageData = new byte[length];
            int totalRead = 0;
            while (totalRead < length) {
                read = input.read(messageData, totalRead, length - totalRead);
                if (read == -1) {
                    throw new PeerException("Peer closed connection during message read");
                }
                totalRead += read;
            }

            ByteBuffer buffer = ByteBuffer.allocate(4 + length);
            buffer.putInt(length);
            buffer.put(messageData);
            buffer.flip();

            PeerMessage message = PeerMessage.decode(buffer);
            log.trace("Received message: {}", message);

            // Update state based on received message
            updateStateFromReceivedMessage(message);

            return message;

        } catch (SocketTimeoutException e) {
            // Timeout is normal, send keep-alive
            return new PeerMessage(PeerMessage.MessageType.KEEP_ALIVE);
        } catch (IOException e) {
            throw new PeerException("Failed to receive message: " + e.getMessage(), e);
        }
    }

    /**
     * Update connection state based on sent message
     */
    private void updateStateFromSentMessage(PeerMessage message) {
        switch (message.getType()) {
            case CHOKE -> amChoking = true;
            case UNCHOKE -> amChoking = false;
            case INTERESTED -> amInterested = true;
            case NOT_INTERESTED -> amInterested = false;
        }
    }

    /**
     * Update connection state based on received message
     */
    private void updateStateFromReceivedMessage(PeerMessage message) {
        switch (message.getType()) {
            case CHOKE -> peerChoking = true;
            case UNCHOKE -> peerChoking = false;
            case INTERESTED -> peerInterested = true;
            case NOT_INTERESTED -> peerInterested = false;
            case BITFIELD -> parseBitfield(message.getPayload());
            case HAVE -> {
                int pieceIndex = message.getHavePieceIndex();
                if (peerPieces != null) {
                    peerPieces.set(pieceIndex);
                }
            }
        }
    }

    /**
     * Parse bitfield message to determine which pieces peer has
     */
    private void parseBitfield(byte[] bitfield) {
        peerPieces = BitSet.valueOf(bitfield);
        log.debug("Peer has {} pieces", peerPieces.cardinality());
    }

    /**
     * Check if peer has a specific piece
     */
    public boolean hasPiece(int pieceIndex) {
        return peerPieces != null && peerPieces.get(pieceIndex);
    }

    /**
     * Request a block from peer
     */
    public void requestBlock(int pieceIndex, int begin, int length) throws PeerException {
        if (peerChoking) {
            throw new PeerException("Cannot request: peer is choking us");
        }
        sendMessage(PeerMessage.request(pieceIndex, begin, length));
    }

    /**
     * Express interest in peer
     */
    public void sendInterested() throws PeerException {
        if (!amInterested) {
            sendMessage(PeerMessage.interested());
        }
    }

    /**
     * Express lack of interest in peer
     */
    public void sendNotInterested() throws PeerException {
        if (amInterested) {
            sendMessage(PeerMessage.notInterested());
        }
    }

    /**
     * Unchoke peer
     */
    public void sendUnchoke() throws PeerException {
        if (amChoking) {
            sendMessage(PeerMessage.unchoke());
        }
    }

    /**
     * Choke peer
     */
    public void sendChoke() throws PeerException {
        if (!amChoking) {
            sendMessage(PeerMessage.choke());
        }
    }

    /**
     * Close connection
     */
    public void close() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.warn("Error closing connection: {}", e.getMessage());
        }
    }

    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getPeerAddress() {
        return host + ":" + port;
    }

    @Override
    public String toString() {
        return "PeerConnection{" + host + ":" + port + "}";
    }
}
