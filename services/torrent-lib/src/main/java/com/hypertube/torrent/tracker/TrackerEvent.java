package com.hypertube.torrent.tracker;

/**
 * Tracker announce events
 */
public enum TrackerEvent {
    NONE,      // Regular announce
    STARTED,   // First announce when download starts
    COMPLETED, // Announce when download completes
    STOPPED    // Announce when client stops/closes
}
