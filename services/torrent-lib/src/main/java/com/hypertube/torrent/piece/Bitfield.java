package com.hypertube.torrent.piece;

import java.util.BitSet;

/**
 * Bitfield representing which pieces we have
 */
public class Bitfield {
    private final BitSet bits;
    private final int numPieces;

    public Bitfield(int numPieces) {
        this.numPieces = numPieces;
        this.bits = new BitSet(numPieces);
    }

    public Bitfield(byte[] bitfield, int numPieces) {
        this.numPieces = numPieces;
        this.bits = BitSet.valueOf(bitfield);
    }

    /**
     * Mark piece as downloaded
     */
    public void setPiece(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= numPieces) {
            throw new IndexOutOfBoundsException("Invalid piece index: " + pieceIndex);
        }
        bits.set(pieceIndex);
    }

    /**
     * Check if we have a piece
     */
    public boolean hasPiece(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= numPieces) {
            return false;
        }
        return bits.get(pieceIndex);
    }

    /**
     * Get number of pieces we have
     */
    public int getPiecesCount() {
        return bits.cardinality();
    }

    /**
     * Get total number of pieces
     */
    public int getTotalPieces() {
        return numPieces;
    }

    /**
     * Check if download is complete
     */
    public boolean isComplete() {
        return bits.cardinality() == numPieces;
    }

    /**
     * Get completion percentage
     */
    public double getCompletionPercentage() {
        return (bits.cardinality() * 100.0) / numPieces;
    }

    /**
     * Get bitfield as byte array (for sending to peers)
     */
    public byte[] toByteArray() {
        return bits.toByteArray();
    }

    /**
     * Find next missing piece index
     */
    public int getNextMissingPiece() {
        return bits.nextClearBit(0);
    }

    /**
     * Find next missing piece that peer has
     */
    public int getNextMissingPiece(Bitfield peerBitfield) {
        for (int i = 0; i < numPieces; i++) {
            if (!hasPiece(i) && peerBitfield.hasPiece(i)) {
                return i;
            }
        }
        return -1; // No missing pieces that peer has
    }

    /**
     * Clear all pieces
     */
    public void clear() {
        bits.clear();
    }

    @Override
    public String toString() {
        return String.format("Bitfield[%d/%d pieces (%.1f%%)]",
            bits.cardinality(), numPieces, getCompletionPercentage());
    }
}
