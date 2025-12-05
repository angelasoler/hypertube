package com.hypertube.torrent.piece;

import lombok.Getter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Represents a single piece of the torrent
 * A piece is divided into blocks (typically 16KB each)
 */
public class Piece {
    @Getter
    private final int index;
    @Getter
    private final int length;
    private final byte[] expectedHash;
    private final byte[] data;
    private final BitSet downloadedBlocks;
    private final int blockSize;
    private final int numBlocks;

    public static final int DEFAULT_BLOCK_SIZE = 16384; // 16 KB

    public Piece(int index, int length, byte[] expectedHash) {
        this(index, length, expectedHash, DEFAULT_BLOCK_SIZE);
    }

    public Piece(int index, int length, byte[] expectedHash, int blockSize) {
        this.index = index;
        this.length = length;
        this.expectedHash = expectedHash;
        this.blockSize = blockSize;
        this.numBlocks = (int) Math.ceil((double) length / blockSize);
        this.data = new byte[length];
        this.downloadedBlocks = new BitSet(numBlocks);
    }

    /**
     * Write block data to piece
     */
    public synchronized boolean writeBlock(int begin, byte[] blockData) {
        if (begin < 0 || begin >= length) {
            return false;
        }

        if (begin + blockData.length > length) {
            return false;
        }

        // Calculate block index
        int blockIndex = begin / blockSize;
        if (downloadedBlocks.get(blockIndex)) {
            // Already have this block
            return true;
        }

        // Copy block data
        System.arraycopy(blockData, 0, data, begin, blockData.length);
        downloadedBlocks.set(blockIndex);

        return true;
    }

    /**
     * Check if piece is complete
     */
    public boolean isComplete() {
        return downloadedBlocks.cardinality() == numBlocks;
    }

    /**
     * Verify piece integrity using SHA-1 hash
     */
    public boolean verify() {
        if (!isComplete()) {
            return false;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] actualHash = digest.digest(data);
            return Arrays.equals(actualHash, expectedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    /**
     * Get piece data (only if complete and verified)
     */
    public byte[] getData() {
        if (!isComplete() || !verify()) {
            return null;
        }
        return Arrays.copyOf(data, length);
    }

    /**
     * Get next block to request
     */
    public BlockRequest getNextBlockRequest() {
        int blockIndex = downloadedBlocks.nextClearBit(0);
        if (blockIndex >= numBlocks) {
            return null; // All blocks downloaded
        }

        int begin = blockIndex * blockSize;
        int blockLength = Math.min(blockSize, length - begin);

        return new BlockRequest(index, begin, blockLength);
    }

    /**
     * Get number of blocks downloaded
     */
    public int getDownloadedBlocks() {
        return downloadedBlocks.cardinality();
    }

    /**
     * Get total number of blocks
     */
    public int getTotalBlocks() {
        return numBlocks;
    }

    /**
     * Get download progress percentage
     */
    public double getProgress() {
        return (downloadedBlocks.cardinality() * 100.0) / numBlocks;
    }

    /**
     * Reset piece (clear all downloaded blocks)
     */
    public void reset() {
        downloadedBlocks.clear();
        Arrays.fill(data, (byte) 0);
    }

    /**
     * Block request information
     */
    @Getter
    public static class BlockRequest {
        private final int pieceIndex;
        private final int begin;
        private final int length;

        public BlockRequest(int pieceIndex, int begin, int length) {
            this.pieceIndex = pieceIndex;
            this.begin = begin;
            this.length = length;
        }

        @Override
        public String toString() {
            return String.format("Block[piece=%d, begin=%d, length=%d]", pieceIndex, begin, length);
        }
    }

    @Override
    public String toString() {
        return String.format("Piece[index=%d, size=%d, progress=%.1f%%, complete=%s]",
            index, length, getProgress(), isComplete());
    }
}
