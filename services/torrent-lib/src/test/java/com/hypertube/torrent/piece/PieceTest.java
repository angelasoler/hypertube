package com.hypertube.torrent.piece;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class PieceTest {

    @Test
    void testCreatePiece() {
        byte[] hash = new byte[20];
        Piece piece = new Piece(0, 262144, hash);

        assertThat(piece.getIndex()).isEqualTo(0);
        assertThat(piece.getLength()).isEqualTo(262144);
        assertThat(piece.isComplete()).isFalse();
        assertThat(piece.getProgress()).isEqualTo(0.0);
    }

    @Test
    void testWriteBlock() {
        byte[] hash = new byte[20];
        Piece piece = new Piece(0, 32768, hash, 16384); // 32KB piece, 16KB blocks

        byte[] block1 = new byte[16384];
        Arrays.fill(block1, (byte) 1);

        boolean written = piece.writeBlock(0, block1);
        assertThat(written).isTrue();
        assertThat(piece.getDownloadedBlocks()).isEqualTo(1);
        assertThat(piece.getTotalBlocks()).isEqualTo(2);
        assertThat(piece.getProgress()).isEqualTo(50.0);
    }

    @Test
    void testWriteMultipleBlocks() {
        byte[] hash = new byte[20];
        Piece piece = new Piece(0, 32768, hash, 16384);

        byte[] block1 = new byte[16384];
        byte[] block2 = new byte[16384];
        Arrays.fill(block1, (byte) 1);
        Arrays.fill(block2, (byte) 2);

        piece.writeBlock(0, block1);
        piece.writeBlock(16384, block2);

        assertThat(piece.isComplete()).isTrue();
        assertThat(piece.getProgress()).isEqualTo(100.0);
    }

    @Test
    void testWriteInvalidBlock() {
        byte[] hash = new byte[20];
        Piece piece = new Piece(0, 16384, hash);

        // Write beyond piece boundary
        byte[] block = new byte[20000];
        boolean written = piece.writeBlock(0, block);
        assertThat(written).isFalse();

        // Negative offset
        written = piece.writeBlock(-1, new byte[100]);
        assertThat(written).isFalse();
    }

    @Test
    void testVerifyPiece() throws NoSuchAlgorithmException {
        // Create piece data
        byte[] data = new byte[16384];
        Arrays.fill(data, (byte) 42);

        // Calculate expected hash
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] expectedHash = digest.digest(data);

        Piece piece = new Piece(0, 16384, expectedHash, 16384);

        // Write the data
        piece.writeBlock(0, data);

        // Verify
        assertThat(piece.isComplete()).isTrue();
        assertThat(piece.verify()).isTrue();
    }

    @Test
    void testVerifyPieceWithWrongData() throws NoSuchAlgorithmException {
        byte[] correctData = new byte[16384];
        Arrays.fill(correctData, (byte) 42);

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] expectedHash = digest.digest(correctData);

        Piece piece = new Piece(0, 16384, expectedHash, 16384);

        // Write wrong data
        byte[] wrongData = new byte[16384];
        Arrays.fill(wrongData, (byte) 99);
        piece.writeBlock(0, wrongData);

        // Verify should fail
        assertThat(piece.isComplete()).isTrue();
        assertThat(piece.verify()).isFalse();
    }

    @Test
    void testGetNextBlockRequest() {
        byte[] hash = new byte[20];
        Piece piece = new Piece(0, 32768, hash, 16384); // 2 blocks

        // First request
        Piece.BlockRequest request = piece.getNextBlockRequest();
        assertThat(request).isNotNull();
        assertThat(request.getPieceIndex()).isEqualTo(0);
        assertThat(request.getBegin()).isEqualTo(0);
        assertThat(request.getLength()).isEqualTo(16384);

        // Write first block
        piece.writeBlock(0, new byte[16384]);

        // Second request
        request = piece.getNextBlockRequest();
        assertThat(request).isNotNull();
        assertThat(request.getBegin()).isEqualTo(16384);
        assertThat(request.getLength()).isEqualTo(16384);

        // Write second block
        piece.writeBlock(16384, new byte[16384]);

        // No more blocks
        request = piece.getNextBlockRequest();
        assertThat(request).isNull();
    }

    @Test
    void testGetNextBlockRequestPartialLastBlock() {
        byte[] hash = new byte[20];
        Piece piece = new Piece(0, 20000, hash, 16384); // Last block is partial

        // Skip first block
        piece.writeBlock(0, new byte[16384]);

        Piece.BlockRequest request = piece.getNextBlockRequest();
        assertThat(request.getBegin()).isEqualTo(16384);
        assertThat(request.getLength()).isEqualTo(3616); // 20000 - 16384
    }

    @Test
    void testGetData() throws NoSuchAlgorithmException {
        byte[] data = new byte[16384];
        Arrays.fill(data, (byte) 123);

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(data);

        Piece piece = new Piece(0, 16384, hash, 16384);

        // Before writing - should return null
        assertThat(piece.getData()).isNull();

        // Write data
        piece.writeBlock(0, data);

        // After writing - should return data
        byte[] retrieved = piece.getData();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isEqualTo(data);
    }

    @Test
    void testReset() {
        byte[] hash = new byte[20];
        Piece piece = new Piece(0, 16384, hash);

        piece.writeBlock(0, new byte[16384]);
        assertThat(piece.getDownloadedBlocks()).isEqualTo(1);

        piece.reset();
        assertThat(piece.getDownloadedBlocks()).isEqualTo(0);
        assertThat(piece.isComplete()).isFalse();
    }

    @Test
    void testToString() {
        byte[] hash = new byte[20];
        Piece piece = new Piece(5, 262144, hash);

        String str = piece.toString();
        assertThat(str).contains("index=5");
        assertThat(str).contains("size=262144");
        assertThat(str).contains("progress=");
        assertThat(str).contains("complete=false");
    }

    @Test
    void testWriteSameBlockTwice() {
        byte[] hash = new byte[20];
        Piece piece = new Piece(0, 32768, hash, 16384);

        byte[] block = new byte[16384];
        Arrays.fill(block, (byte) 1);

        piece.writeBlock(0, block);
        assertThat(piece.getDownloadedBlocks()).isEqualTo(1);

        // Write same block again
        piece.writeBlock(0, block);
        assertThat(piece.getDownloadedBlocks()).isEqualTo(1); // Should still be 1
    }

    @Test
    void testCalculateNumBlocks() {
        byte[] hash = new byte[20];

        // Exact multiple
        Piece piece1 = new Piece(0, 32768, hash, 16384);
        assertThat(piece1.getTotalBlocks()).isEqualTo(2);

        // With remainder
        Piece piece2 = new Piece(0, 40000, hash, 16384);
        assertThat(piece2.getTotalBlocks()).isEqualTo(3);

        // Single block
        Piece piece3 = new Piece(0, 8192, hash, 16384);
        assertThat(piece3.getTotalBlocks()).isEqualTo(1);
    }
}
