package com.hypertube.torrent.tracker;

/**
 * Exception thrown when tracker communication fails
 */
public class TrackerException extends Exception {

    public TrackerException(String message) {
        super(message);
    }

    public TrackerException(String message, Throwable cause) {
        super(message, cause);
    }
}
