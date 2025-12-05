package com.hypertube.torrent.tracker;

import com.hypertube.torrent.TorrentMetadata;
import com.hypertube.torrent.bencode.BencodeDecoder;
import com.hypertube.torrent.bencode.BencodeException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * BitTorrent tracker client for HTTP/HTTPS trackers
 */
@Slf4j
public class TrackerClient {
    private final HttpClient httpClient;
    private final byte[] peerId;
    private final int port;

    private static final Random RANDOM = new Random();

    public TrackerClient(int port) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.peerId = generatePeerId();
        this.port = port;
    }

    /**
     * Announce to tracker and get peer list
     */
    public TrackerResponse announce(TorrentMetadata metadata, TrackerEvent event,
                                    long downloaded, long uploaded, long left) throws TrackerException {
        if (metadata.getTrackers().isEmpty()) {
            throw new TrackerException("No trackers available");
        }

        // Try each tracker until one succeeds
        TrackerException lastException = null;
        for (String trackerUrl : metadata.getTrackers()) {
            if (!trackerUrl.startsWith("http://") && !trackerUrl.startsWith("https://")) {
                log.debug("Skipping non-HTTP tracker: {}", trackerUrl);
                continue;
            }

            try {
                return announceToTracker(trackerUrl, metadata, event, downloaded, uploaded, left);
            } catch (TrackerException e) {
                log.warn("Failed to announce to tracker {}: {}", trackerUrl, e.getMessage());
                lastException = e;
            }
        }

        throw lastException != null ? lastException :
            new TrackerException("All trackers failed or no HTTP trackers available");
    }

    /**
     * Announce to a specific tracker
     */
    private TrackerResponse announceToTracker(String trackerUrl, TorrentMetadata metadata,
                                              TrackerEvent event, long downloaded,
                                              long uploaded, long left) throws TrackerException {
        try {
            String url = buildTrackerUrl(trackerUrl, metadata, event, downloaded, uploaded, left);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            log.debug("Announcing to tracker: {}", url);

            HttpResponse<byte[]> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new TrackerException("Tracker returned status " + response.statusCode());
            }

            return parseTrackerResponse(response.body());

        } catch (IOException | InterruptedException e) {
            throw new TrackerException("Failed to contact tracker: " + e.getMessage(), e);
        } catch (BencodeException e) {
            throw new TrackerException("Failed to parse tracker response: " + e.getMessage(), e);
        }
    }

    /**
     * Build tracker announce URL with all required parameters
     */
    private String buildTrackerUrl(String baseUrl, TorrentMetadata metadata, TrackerEvent event,
                                   long downloaded, long uploaded, long left) {
        StringBuilder url = new StringBuilder(baseUrl);

        // Add separator
        url.append(baseUrl.contains("?") ? "&" : "?");

        // Required parameters
        url.append("info_hash=").append(urlEncode(metadata.getInfoHash()));
        url.append("&peer_id=").append(urlEncode(peerId));
        url.append("&port=").append(port);
        url.append("&uploaded=").append(uploaded);
        url.append("&downloaded=").append(downloaded);
        url.append("&left=").append(left);
        url.append("&compact=1"); // Request compact peer list
        url.append("&numwant=50"); // Request 50 peers

        // Optional event parameter
        if (event != null && event != TrackerEvent.NONE) {
            url.append("&event=").append(event.name().toLowerCase());
        }

        return url.toString();
    }

    /**
     * Parse tracker response
     */
    private TrackerResponse parseTrackerResponse(byte[] responseData) throws BencodeException {
        Map<String, Object> response = (Map<String, Object>) BencodeDecoder.decode(responseData);

        // Check for tracker error
        String failureReason = BencodeDecoder.getString(response, "failure reason");
        if (failureReason != null) {
            throw new BencodeException("Tracker error: " + failureReason);
        }

        // Parse response fields
        Long interval = BencodeDecoder.getLong(response, "interval");
        Long complete = BencodeDecoder.getLong(response, "complete");
        Long incomplete = BencodeDecoder.getLong(response, "incomplete");

        // Parse peers
        List<Peer> peers = new ArrayList<>();
        Object peersObj = response.get("peers");

        if (peersObj instanceof byte[]) {
            // Compact peer format: 6 bytes per peer (4 bytes IP + 2 bytes port)
            peers = parseCompactPeers((byte[]) peersObj);
        } else if (peersObj instanceof List) {
            // Dictionary peer format
            peers = parseDictionaryPeers((List<Object>) peersObj);
        }

        return TrackerResponse.builder()
            .interval(interval != null ? interval.intValue() : 1800)
            .seeders(complete != null ? complete.intValue() : 0)
            .leechers(incomplete != null ? incomplete.intValue() : 0)
            .peers(peers)
            .build();
    }

    /**
     * Parse compact peer format (6 bytes per peer)
     */
    private List<Peer> parseCompactPeers(byte[] peersData) {
        List<Peer> peers = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(peersData);

        while (buffer.remaining() >= 6) {
            // Parse IP (4 bytes, unsigned)
            int ip1 = buffer.get() & 0xFF;
            int ip2 = buffer.get() & 0xFF;
            int ip3 = buffer.get() & 0xFF;
            int ip4 = buffer.get() & 0xFF;
            String ip = ip1 + "." + ip2 + "." + ip3 + "." + ip4;

            // Parse port (2 bytes, unsigned)
            int port = buffer.getShort() & 0xFFFF;

            peers.add(new Peer(ip, port, null));
        }

        return peers;
    }

    /**
     * Parse dictionary peer format
     */
    private List<Peer> parseDictionaryPeers(List<Object> peersList) {
        List<Peer> peers = new ArrayList<>();

        for (Object peerObj : peersList) {
            if (peerObj instanceof Map) {
                Map<String, Object> peerMap = (Map<String, Object>) peerObj;
                String ip = BencodeDecoder.getString(peerMap, "ip");
                Long port = BencodeDecoder.getLong(peerMap, "port");
                byte[] peerId = (byte[]) peerMap.get("peer id");

                if (ip != null && port != null) {
                    peers.add(new Peer(ip, port.intValue(), peerId));
                }
            }
        }

        return peers;
    }

    /**
     * Generate random peer ID
     * Format: -HT0100-<12 random chars>
     * HT = HyperTube, 0100 = version 1.0.0
     */
    private byte[] generatePeerId() {
        String prefix = "-HT0100-";
        byte[] peerId = new byte[20];

        // Add prefix
        byte[] prefixBytes = prefix.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(prefixBytes, 0, peerId, 0, prefixBytes.length);

        // Add random bytes
        for (int i = prefixBytes.length; i < peerId.length; i++) {
            peerId[i] = (byte) (RANDOM.nextInt(256));
        }

        return peerId;
    }

    /**
     * URL encode bytes (for info_hash and peer_id)
     */
    private String urlEncode(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // Use percent encoding for all bytes
            sb.append(String.format("%%%02X", b & 0xFF));
        }
        return sb.toString();
    }

    public byte[] getPeerId() {
        return peerId;
    }
}
