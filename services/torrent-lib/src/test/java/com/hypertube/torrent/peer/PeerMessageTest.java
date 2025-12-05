package com.hypertube.torrent.peer;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.*;

class PeerMessageTest {

    @Test
    void testEncodeDecodeKeepAlive() throws PeerException {
        PeerMessage message = new PeerMessage(PeerMessage.MessageType.KEEP_ALIVE);
        byte[] encoded = message.encode();

        assertThat(encoded).hasSize(4);
        assertThat(ByteBuffer.wrap(encoded).getInt()).isEqualTo(0);

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.KEEP_ALIVE);
    }

    @Test
    void testEncodeDecodeChoke() throws PeerException {
        PeerMessage message = PeerMessage.choke();
        byte[] encoded = message.encode();

        // Length (4) + ID (1) = 5 bytes
        assertThat(encoded).hasSize(5);

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.CHOKE);
    }

    @Test
    void testEncodeDecodeUnchoke() throws PeerException {
        PeerMessage message = PeerMessage.unchoke();
        byte[] encoded = message.encode();

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.UNCHOKE);
    }

    @Test
    void testEncodeDecodeInterested() throws PeerException {
        PeerMessage message = PeerMessage.interested();
        byte[] encoded = message.encode();

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.INTERESTED);
    }

    @Test
    void testEncodeDecodeNotInterested() throws PeerException {
        PeerMessage message = PeerMessage.notInterested();
        byte[] encoded = message.encode();

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.NOT_INTERESTED);
    }

    @Test
    void testEncodeDecodeHave() throws PeerException {
        PeerMessage message = PeerMessage.have(42);
        byte[] encoded = message.encode();

        // Length (4) + ID (1) + piece index (4) = 9 bytes
        assertThat(encoded).hasSize(9);

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.HAVE);
        assertThat(decoded.getHavePieceIndex()).isEqualTo(42);
    }

    @Test
    void testEncodeDecodeBitfield() throws PeerException {
        byte[] bitfield = new byte[]{(byte) 0xFF, (byte) 0x00, (byte) 0xAA};
        PeerMessage message = PeerMessage.bitfield(bitfield);
        byte[] encoded = message.encode();

        // Length (4) + ID (1) + bitfield (3) = 8 bytes
        assertThat(encoded).hasSize(8);

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.BITFIELD);
        assertThat(decoded.getPayload()).isEqualTo(bitfield);
    }

    @Test
    void testEncodeDecodeRequest() throws PeerException {
        PeerMessage message = PeerMessage.request(5, 16384, 16384);
        byte[] encoded = message.encode();

        // Length (4) + ID (1) + index (4) + begin (4) + length (4) = 17 bytes
        assertThat(encoded).hasSize(17);

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.REQUEST);

        PeerMessage.BlockInfo blockInfo = decoded.getBlockInfo();
        assertThat(blockInfo.getPieceIndex()).isEqualTo(5);
        assertThat(blockInfo.getBegin()).isEqualTo(16384);
        assertThat(blockInfo.getLength()).isEqualTo(16384);
    }

    @Test
    void testEncodeDecodePiece() throws PeerException {
        byte[] blockData = new byte[]{1, 2, 3, 4, 5};
        PeerMessage message = PeerMessage.piece(10, 0, blockData);
        byte[] encoded = message.encode();

        // Length (4) + ID (1) + index (4) + begin (4) + block (5) = 18 bytes
        assertThat(encoded).hasSize(18);

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.PIECE);

        PeerMessage.BlockInfo blockInfo = decoded.getBlockInfo();
        assertThat(blockInfo.getPieceIndex()).isEqualTo(10);
        assertThat(blockInfo.getBegin()).isEqualTo(0);
        assertThat(blockInfo.getLength()).isEqualTo(5);
        assertThat(blockInfo.getData()).isEqualTo(blockData);
    }

    @Test
    void testEncodeDecodeCancel() throws PeerException {
        PeerMessage message = PeerMessage.cancel(3, 32768, 16384);
        byte[] encoded = message.encode();

        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PeerMessage decoded = PeerMessage.decode(buffer);

        assertThat(decoded.getType()).isEqualTo(PeerMessage.MessageType.CANCEL);

        PeerMessage.BlockInfo blockInfo = decoded.getBlockInfo();
        assertThat(blockInfo.getPieceIndex()).isEqualTo(3);
        assertThat(blockInfo.getBegin()).isEqualTo(32768);
        assertThat(blockInfo.getLength()).isEqualTo(16384);
    }

    @Test
    void testMessageTypeFromId() {
        assertThat(PeerMessage.MessageType.fromId(0)).isEqualTo(PeerMessage.MessageType.CHOKE);
        assertThat(PeerMessage.MessageType.fromId(1)).isEqualTo(PeerMessage.MessageType.UNCHOKE);
        assertThat(PeerMessage.MessageType.fromId(4)).isEqualTo(PeerMessage.MessageType.HAVE);
        assertThat(PeerMessage.MessageType.fromId(7)).isEqualTo(PeerMessage.MessageType.PIECE);
    }

    @Test
    void testInvalidMessageType() {
        assertThatThrownBy(() -> PeerMessage.MessageType.fromId(99))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown message type");
    }

    @Test
    void testNotEnoughDataForLength() {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.flip();

        assertThatThrownBy(() -> PeerMessage.decode(buffer))
            .isInstanceOf(PeerException.class)
            .hasMessageContaining("Not enough data for message length");
    }

    @Test
    void testNotEnoughDataForPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.putInt(10); // Length says 10 bytes, but we only have 1
        buffer.put((byte) 0);
        buffer.flip();

        assertThatThrownBy(() -> PeerMessage.decode(buffer))
            .isInstanceOf(PeerException.class)
            .hasMessageContaining("Not enough data for message payload");
    }

    @Test
    void testToString() {
        PeerMessage keepAlive = new PeerMessage(PeerMessage.MessageType.KEEP_ALIVE);
        assertThat(keepAlive.toString()).isEqualTo("KeepAlive");

        PeerMessage choke = PeerMessage.choke();
        assertThat(choke.toString()).contains("CHOKE");

        PeerMessage have = PeerMessage.have(5);
        assertThat(have.toString()).contains("HAVE");
        assertThat(have.toString()).contains("4 bytes");
    }

    @Test
    void testRoundTripAllMessageTypes() throws PeerException {
        PeerMessage[] messages = {
            PeerMessage.choke(),
            PeerMessage.unchoke(),
            PeerMessage.interested(),
            PeerMessage.notInterested(),
            PeerMessage.have(123),
            PeerMessage.bitfield(new byte[]{1, 2, 3}),
            PeerMessage.request(5, 0, 16384),
            PeerMessage.piece(7, 16384, new byte[]{9, 8, 7}),
            PeerMessage.cancel(2, 32768, 16384)
        };

        for (PeerMessage original : messages) {
            byte[] encoded = original.encode();
            ByteBuffer buffer = ByteBuffer.wrap(encoded);
            PeerMessage decoded = PeerMessage.decode(buffer);

            assertThat(decoded.getType()).isEqualTo(original.getType());

            if (original.getPayload() != null) {
                assertThat(decoded.getPayload()).isEqualTo(original.getPayload());
            }
        }
    }
}
