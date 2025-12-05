package com.hypertube.torrent.peer;

/**
 * Exception thrown during peer communication
 */
public class PeerException extends Exception {

    public PeerException(String message) {
        super(message);
    }

    public PeerException(String message, Throwable cause) {
        super(message, cause);
    }
}
