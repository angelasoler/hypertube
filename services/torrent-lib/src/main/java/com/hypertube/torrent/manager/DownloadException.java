package com.hypertube.torrent.manager;

/**
 * Exception thrown during download operations
 */
public class DownloadException extends Exception {

    public DownloadException(String message) {
        super(message);
    }

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
