package com.hypertube.torrent.peer;

import lombok.Data;

import java.nio.ByteBuffer;

/**
 * BitTorrent peer wire protocol message
 *
 * Message format: <length><id><payload>
 * - length: 4 bytes (big-endian)
 * - id: 1 byte (message type)
 * - payload: variable length
 */
@Data
public class PeerMessage {
    private MessageType type;
    private byte[] payload;

    public enum MessageType {
        CHOKE(0),           // Sender is choking receiver
        UNCHOKE(1),         // Sender is not choking receiver
        INTERESTED(2),      // Sender is interested in receiver
        NOT_INTERESTED(3),  // Sender is not interested in receiver
        HAVE(4),            // Sender has a piece (4-byte piece index)
        BITFIELD(5),        // Sender's piece availability bitmap
        REQUEST(6),         // Request a block (index, begin, length)
        PIECE(7),           // Block data (index, begin, block)
        CANCEL(8),          // Cancel a request (index, begin, length)
        KEEP_ALIVE(-1);     // Keep connection alive (length=0, no id)

        private final int id;

        MessageType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static MessageType fromId(int id) {
            for (MessageType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown message type: " + id);
        }
    }

    public PeerMessage(MessageType type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    public PeerMessage(MessageType type) {
        this(type, new byte[0]);
    }

    /**
     * Encode message to wire format
     */
    public byte[] encode() {
        if (type == MessageType.KEEP_ALIVE) {
            // Keep-alive: length=0
            return new byte[]{0, 0, 0, 0};
        }

        int length = 1 + (payload != null ? payload.length : 0);
        ByteBuffer buffer = ByteBuffer.allocate(4 + length);

        // Length prefix (big-endian)
        buffer.putInt(length);

        // Message ID
        buffer.put((byte) type.getId());

        // Payload
        if (payload != null && payload.length > 0) {
            buffer.put(payload);
        }

        return buffer.array();
    }

    /**
     * Decode message from wire format
     */
    public static PeerMessage decode(ByteBuffer buffer) throws PeerException {
        if (buffer.remaining() < 4) {
            throw new PeerException("Not enough data for message length");
        }

        int length = buffer.getInt();

        if (length == 0) {
            return new PeerMessage(MessageType.KEEP_ALIVE);
        }

        if (buffer.remaining() < length) {
            throw new PeerException("Not enough data for message payload");
        }

        byte typeId = buffer.get();
        MessageType type = MessageType.fromId(typeId);

        byte[] payload = new byte[length - 1];
        if (payload.length > 0) {
            buffer.get(payload);
        }

        return new PeerMessage(type, payload);
    }

    /**
     * Create CHOKE message
     */
    public static PeerMessage choke() {
        return new PeerMessage(MessageType.CHOKE);
    }

    /**
     * Create UNCHOKE message
     */
    public static PeerMessage unchoke() {
        return new PeerMessage(MessageType.UNCHOKE);
    }

    /**
     * Create INTERESTED message
     */
    public static PeerMessage interested() {
        return new PeerMessage(MessageType.INTERESTED);
    }

    /**
     * Create NOT_INTERESTED message
     */
    public static PeerMessage notInterested() {
        return new PeerMessage(MessageType.NOT_INTERESTED);
    }

    /**
     * Create HAVE message
     */
    public static PeerMessage have(int pieceIndex) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(pieceIndex);
        return new PeerMessage(MessageType.HAVE, buffer.array());
    }

    /**
     * Create BITFIELD message
     */
    public static PeerMessage bitfield(byte[] bitfield) {
        return new PeerMessage(MessageType.BITFIELD, bitfield);
    }

    /**
     * Create REQUEST message
     * @param pieceIndex Piece index
     * @param begin Offset within piece
     * @param length Block length (typically 16KB)
     */
    public static PeerMessage request(int pieceIndex, int begin, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(pieceIndex);
        buffer.putInt(begin);
        buffer.putInt(length);
        return new PeerMessage(MessageType.REQUEST, buffer.array());
    }

    /**
     * Create PIECE message
     */
    public static PeerMessage piece(int pieceIndex, int begin, byte[] block) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + block.length);
        buffer.putInt(pieceIndex);
        buffer.putInt(begin);
        buffer.put(block);
        return new PeerMessage(MessageType.PIECE, buffer.array());
    }

    /**
     * Create CANCEL message
     */
    public static PeerMessage cancel(int pieceIndex, int begin, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(pieceIndex);
        buffer.putInt(begin);
        buffer.putInt(length);
        return new PeerMessage(MessageType.CANCEL, buffer.array());
    }

    /**
     * Parse HAVE message payload
     */
    public int getHavePieceIndex() {
        if (type != MessageType.HAVE || payload.length != 4) {
            throw new IllegalStateException("Not a valid HAVE message");
        }
        return ByteBuffer.wrap(payload).getInt();
    }

    /**
     * Parse REQUEST/PIECE/CANCEL message payload
     */
    public BlockInfo getBlockInfo() {
        if ((type != MessageType.REQUEST && type != MessageType.CANCEL && type != MessageType.PIECE)
            || payload.length < 8) {
            throw new IllegalStateException("Not a valid block message");
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int pieceIndex = buffer.getInt();
        int begin = buffer.getInt();

        if (type == MessageType.PIECE) {
            byte[] block = new byte[payload.length - 8];
            buffer.get(block);
            return new BlockInfo(pieceIndex, begin, block.length, block);
        } else {
            int length = buffer.getInt();
            return new BlockInfo(pieceIndex, begin, length, null);
        }
    }

    @Data
    public static class BlockInfo {
        private final int pieceIndex;
        private final int begin;
        private final int length;
        private final byte[] data;  // Only for PIECE messages
    }

    @Override
    public String toString() {
        if (type == MessageType.KEEP_ALIVE) {
            return "KeepAlive";
        }
        return type.name() + (payload != null ? " (" + payload.length + " bytes)" : "");
    }
}
