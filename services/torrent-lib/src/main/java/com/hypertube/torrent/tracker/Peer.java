package com.hypertube.torrent.tracker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BitTorrent peer information
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Peer {
    private String ip;
    private int port;
    private byte[] peerId;

    @Override
    public String toString() {
        return ip + ":" + port;
    }
}
