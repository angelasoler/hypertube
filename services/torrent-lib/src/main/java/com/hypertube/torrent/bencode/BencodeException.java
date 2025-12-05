package com.hypertube.torrent.bencode;

/**
 * Exception thrown when bencode encoding/decoding fails
 */
public class BencodeException extends Exception {

    public BencodeException(String message) {
        super(message);
    }

    public BencodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
