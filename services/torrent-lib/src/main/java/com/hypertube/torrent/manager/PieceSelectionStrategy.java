package com.hypertube.torrent.manager;

/**
 * Strategy for selecting which piece to download next
 */
public enum PieceSelectionStrategy {
    /**
     * Download pieces sequentially from first to last.
     * Best for video streaming where sequential playback is needed.
     */
    SEQUENTIAL,

    /**
     * Download rarest pieces first.
     * Better for overall swarm health and completing downloads.
     */
    RAREST_FIRST
}
