package com.hypertube.torrent;

import com.hypertube.torrent.bencode.BencodeDecoder;
import com.hypertube.torrent.bencode.BencodeEncoder;
import com.hypertube.torrent.bencode.BencodeException;
import lombok.Builder;
import lombok.Data;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Torrent metadata extracted from .torrent file or magnet link
 */
@Data
@Builder
public class TorrentMetadata {
    private String name;
    private byte[] infoHash;
    private List<String> trackers;
    private long totalSize;
    private long pieceLength;
    private List<FileInfo> files;
    private int numPieces;
    private byte[] pieces; // SHA-1 hashes of all pieces concatenated

    @Data
    @Builder
    public static class FileInfo {
        private String path;
        private long length;
    }

    /**
     * Parse torrent metadata from .torrent file bytes
     */
    public static TorrentMetadata fromTorrentFile(byte[] torrentData) throws BencodeException {
        Map<String, Object> root = (Map<String, Object>) BencodeDecoder.decode(torrentData);
        Map<String, Object> info = BencodeDecoder.getMap(root, "info");

        if (info == null) {
            throw new BencodeException("Missing 'info' dictionary in torrent file");
        }

        // Calculate info hash
        byte[] infoHash = calculateInfoHash(info);

        // Parse trackers
        List<String> trackers = new ArrayList<>();
        String announceUrl = BencodeDecoder.getString(root, "announce");
        if (announceUrl != null) {
            trackers.add(announceUrl);
        }

        // Multi-tracker support
        List<Object> announceList = BencodeDecoder.getList(root, "announce-list");
        if (announceList != null) {
            for (Object tier : announceList) {
                if (tier instanceof List) {
                    for (Object tracker : (List<?>) tier) {
                        String trackerUrl = BencodeDecoder.bytesToString(tracker);
                        if (trackerUrl != null && !trackers.contains(trackerUrl)) {
                            trackers.add(trackerUrl);
                        }
                    }
                }
            }
        }

        // Parse file info
        String name = BencodeDecoder.getString(info, "name");
        Long pieceLength = BencodeDecoder.getLong(info, "piece length");
        byte[] pieces = (byte[]) info.get("pieces");

        if (name == null || pieceLength == null || pieces == null) {
            throw new BencodeException("Missing required fields in info dictionary");
        }

        int numPieces = pieces.length / 20; // Each SHA-1 hash is 20 bytes

        // Parse files (single or multi-file torrent)
        List<FileInfo> files = new ArrayList<>();
        long totalSize = 0;

        List<Object> filesList = BencodeDecoder.getList(info, "files");
        if (filesList != null) {
            // Multi-file torrent
            for (Object fileObj : filesList) {
                if (fileObj instanceof Map) {
                    Map<String, Object> fileMap = (Map<String, Object>) fileObj;
                    Long length = BencodeDecoder.getLong(fileMap, "length");
                    List<Object> pathList = BencodeDecoder.getList(fileMap, "path");

                    if (length != null && pathList != null) {
                        StringBuilder pathBuilder = new StringBuilder();
                        for (Object pathPart : pathList) {
                            if (pathBuilder.length() > 0) {
                                pathBuilder.append("/");
                            }
                            pathBuilder.append(BencodeDecoder.bytesToString(pathPart));
                        }

                        files.add(FileInfo.builder()
                            .path(name + "/" + pathBuilder.toString())
                            .length(length)
                            .build());
                        totalSize += length;
                    }
                }
            }
        } else {
            // Single-file torrent
            Long length = BencodeDecoder.getLong(info, "length");
            if (length != null) {
                files.add(FileInfo.builder()
                    .path(name)
                    .length(length)
                    .build());
                totalSize = length;
            }
        }

        return TorrentMetadata.builder()
            .name(name)
            .infoHash(infoHash)
            .trackers(trackers)
            .totalSize(totalSize)
            .pieceLength(pieceLength)
            .files(files)
            .numPieces(numPieces)
            .pieces(pieces)
            .build();
    }

    /**
     * Parse torrent metadata from magnet link
     * Format: magnet:?xt=urn:btih:<info-hash>&dn=<name>&tr=<tracker>
     */
    public static TorrentMetadata fromMagnetLink(String magnetLink) throws BencodeException {
        if (!magnetLink.startsWith("magnet:?")) {
            throw new BencodeException("Invalid magnet link format");
        }

        String[] params = magnetLink.substring(8).split("&");
        byte[] infoHash = null;
        String name = null;
        List<String> trackers = new ArrayList<>();

        for (String param : params) {
            String[] kv = param.split("=", 2);
            if (kv.length != 2) continue;

            String key = kv[0];
            String value = urlDecode(kv[1]);

            switch (key) {
                case "xt":
                    if (value.startsWith("urn:btih:")) {
                        String hash = value.substring(9);
                        infoHash = hexToBytes(hash);
                    }
                    break;
                case "dn":
                    name = value;
                    break;
                case "tr":
                    trackers.add(value);
                    break;
            }
        }

        if (infoHash == null) {
            throw new BencodeException("Magnet link missing info hash");
        }

        return TorrentMetadata.builder()
            .name(name != null ? name : "Unknown")
            .infoHash(infoHash)
            .trackers(trackers)
            .build();
    }

    /**
     * Calculate SHA-1 info hash from info dictionary
     */
    private static byte[] calculateInfoHash(Map<String, Object> info) throws BencodeException {
        try {
            byte[] infoBytes = BencodeEncoder.encode(info);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(infoBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new BencodeException("SHA-1 algorithm not available", e);
        }
    }

    /**
     * Convert info hash to hex string
     */
    public String getInfoHashHex() {
        return bytesToHex(infoHash);
    }

    /**
     * Convert info hash to URL-encoded format for tracker requests
     */
    public String getInfoHashUrlEncoded() {
        return urlEncode(infoHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private static String urlEncode(byte[] bytes) {
        try {
            return URLEncoder.encode(new String(bytes, StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * Get the SHA-1 hash for a specific piece
     */
    public byte[] getPieceHash(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= numPieces) {
            throw new IndexOutOfBoundsException("Invalid piece index: " + pieceIndex);
        }

        if (pieces == null) {
            throw new IllegalStateException("Piece hashes not available (magnet link)");
        }

        byte[] hash = new byte[20];
        System.arraycopy(pieces, pieceIndex * 20, hash, 0, 20);
        return hash;
    }

    /**
     * Get the length of a specific piece
     * The last piece may be shorter than pieceLength
     */
    public int getPieceLength(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= numPieces) {
            throw new IndexOutOfBoundsException("Invalid piece index: " + pieceIndex);
        }

        // Last piece may be shorter
        if (pieceIndex == numPieces - 1) {
            long lastPieceLength = totalSize - (pieceIndex * pieceLength);
            return (int) lastPieceLength;
        }

        return (int) pieceLength;
    }
}
