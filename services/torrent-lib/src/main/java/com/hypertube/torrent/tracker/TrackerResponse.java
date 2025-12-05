package com.hypertube.torrent.tracker;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response from tracker announce
 */
@Data
@Builder
public class TrackerResponse {
    private int interval;      // Time to wait before next announce (seconds)
    private int seeders;       // Number of peers with complete file
    private int leechers;      // Number of peers downloading
    private List<Peer> peers;  // List of peers
}
