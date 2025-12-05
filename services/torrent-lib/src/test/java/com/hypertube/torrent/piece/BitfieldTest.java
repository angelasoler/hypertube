package com.hypertube.torrent.piece;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BitfieldTest {

    @Test
    void testCreateEmptyBitfield() {
        Bitfield bitfield = new Bitfield(100);

        assertThat(bitfield.getTotalPieces()).isEqualTo(100);
        assertThat(bitfield.getPiecesCount()).isEqualTo(0);
        assertThat(bitfield.isComplete()).isFalse();
        assertThat(bitfield.getCompletionPercentage()).isEqualTo(0.0);
    }

    @Test
    void testSetPiece() {
        Bitfield bitfield = new Bitfield(10);

        bitfield.setPiece(0);
        bitfield.setPiece(5);
        bitfield.setPiece(9);

        assertThat(bitfield.hasPiece(0)).isTrue();
        assertThat(bitfield.hasPiece(5)).isTrue();
        assertThat(bitfield.hasPiece(9)).isTrue();
        assertThat(bitfield.hasPiece(1)).isFalse();
        assertThat(bitfield.getPiecesCount()).isEqualTo(3);
    }

    @Test
    void testSetInvalidPiece() {
        Bitfield bitfield = new Bitfield(10);

        assertThatThrownBy(() -> bitfield.setPiece(-1))
            .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> bitfield.setPiece(10))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void testHasInvalidPiece() {
        Bitfield bitfield = new Bitfield(10);

        assertThat(bitfield.hasPiece(-1)).isFalse();
        assertThat(bitfield.hasPiece(100)).isFalse();
    }

    @Test
    void testCompletion() {
        Bitfield bitfield = new Bitfield(5);

        for (int i = 0; i < 5; i++) {
            bitfield.setPiece(i);
        }

        assertThat(bitfield.isComplete()).isTrue();
        assertThat(bitfield.getCompletionPercentage()).isEqualTo(100.0);
    }

    @Test
    void testPartialCompletion() {
        Bitfield bitfield = new Bitfield(10);

        bitfield.setPiece(0);
        bitfield.setPiece(1);
        bitfield.setPiece(2);

        assertThat(bitfield.getCompletionPercentage()).isEqualTo(30.0);
    }

    @Test
    void testGetNextMissingPiece() {
        Bitfield bitfield = new Bitfield(10);

        bitfield.setPiece(0);
        bitfield.setPiece(1);
        bitfield.setPiece(2);

        int next = bitfield.getNextMissingPiece();
        assertThat(next).isEqualTo(3);

        bitfield.setPiece(3);
        next = bitfield.getNextMissingPiece();
        assertThat(next).isEqualTo(4);
    }

    @Test
    void testGetNextMissingPieceThatPeerHas() {
        Bitfield ourBitfield = new Bitfield(10);
        Bitfield peerBitfield = new Bitfield(10);

        // We have pieces 0, 1
        ourBitfield.setPiece(0);
        ourBitfield.setPiece(1);

        // Peer has pieces 1, 2, 5
        peerBitfield.setPiece(1);
        peerBitfield.setPiece(2);
        peerBitfield.setPiece(5);

        // Next missing piece that peer has is 2
        int next = ourBitfield.getNextMissingPiece(peerBitfield);
        assertThat(next).isEqualTo(2);

        // After getting piece 2
        ourBitfield.setPiece(2);
        next = ourBitfield.getNextMissingPiece(peerBitfield);
        assertThat(next).isEqualTo(5);
    }

    @Test
    void testGetNextMissingPieceWhenPeerHasNothing() {
        Bitfield ourBitfield = new Bitfield(10);
        Bitfield peerBitfield = new Bitfield(10);

        ourBitfield.setPiece(0);

        // Peer has no pieces
        int next = ourBitfield.getNextMissingPiece(peerBitfield);
        assertThat(next).isEqualTo(-1);
    }

    @Test
    void testToByteArray() {
        Bitfield bitfield = new Bitfield(16);

        bitfield.setPiece(0);
        bitfield.setPiece(1);
        bitfield.setPiece(8);

        byte[] bytes = bitfield.toByteArray();
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);
    }

    @Test
    void testCreateFromByteArray() {
        byte[] bytes = new byte[]{(byte) 0xFF, 0x00};
        Bitfield bitfield = new Bitfield(bytes, 16);

        // First 8 bits should be set
        for (int i = 0; i < 8; i++) {
            assertThat(bitfield.hasPiece(i)).isTrue();
        }

        // Next 8 bits should be unset
        for (int i = 8; i < 16; i++) {
            assertThat(bitfield.hasPiece(i)).isFalse();
        }
    }

    @Test
    void testClear() {
        Bitfield bitfield = new Bitfield(10);

        bitfield.setPiece(0);
        bitfield.setPiece(5);
        bitfield.setPiece(9);

        assertThat(bitfield.getPiecesCount()).isEqualTo(3);

        bitfield.clear();

        assertThat(bitfield.getPiecesCount()).isEqualTo(0);
        assertThat(bitfield.hasPiece(0)).isFalse();
        assertThat(bitfield.hasPiece(5)).isFalse();
        assertThat(bitfield.hasPiece(9)).isFalse();
    }

    @Test
    void testToString() {
        Bitfield bitfield = new Bitfield(100);

        bitfield.setPiece(0);
        bitfield.setPiece(50);
        bitfield.setPiece(99);

        String str = bitfield.toString();
        assertThat(str).contains("3/100");
        assertThat(str).contains("3.0%");
    }

    @Test
    void testRoundTripByteArray() {
        Bitfield original = new Bitfield(64);

        original.setPiece(0);
        original.setPiece(7);
        original.setPiece(15);
        original.setPiece(31);
        original.setPiece(63);

        byte[] bytes = original.toByteArray();
        Bitfield restored = new Bitfield(bytes, 64);

        assertThat(restored.hasPiece(0)).isTrue();
        assertThat(restored.hasPiece(7)).isTrue();
        assertThat(restored.hasPiece(15)).isTrue();
        assertThat(restored.hasPiece(31)).isTrue();
        assertThat(restored.hasPiece(63)).isTrue();
        assertThat(restored.getPiecesCount()).isEqualTo(5);
    }
}
