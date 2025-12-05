package com.hypertube.torrent.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bencode encoder for BitTorrent protocol
 */
public class BencodeEncoder {

    /**
     * Encode object to bencode format
     */
    public static byte[] encode(Object obj) throws BencodeException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            encode(obj, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new BencodeException("Failed to encode bencode data", e);
        }
    }

    /**
     * Encode object to output stream
     */
    private static void encode(Object obj, ByteArrayOutputStream output) throws BencodeException, IOException {
        if (obj instanceof Long || obj instanceof Integer) {
            encodeInteger(((Number) obj).longValue(), output);
        } else if (obj instanceof String) {
            encodeString(((String) obj).getBytes(StandardCharsets.UTF_8), output);
        } else if (obj instanceof byte[]) {
            encodeString((byte[]) obj, output);
        } else if (obj instanceof List) {
            encodeList((List<?>) obj, output);
        } else if (obj instanceof Map) {
            encodeDictionary((Map<?, ?>) obj, output);
        } else {
            throw new BencodeException("Unsupported type for encoding: " +
                (obj != null ? obj.getClass().getName() : "null"));
        }
    }

    /**
     * Encode integer: i<number>e
     */
    private static void encodeInteger(long value, ByteArrayOutputStream output) throws IOException {
        output.write('i');
        output.write(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
        output.write('e');
    }

    /**
     * Encode string: <length>:<string>
     */
    private static void encodeString(byte[] value, ByteArrayOutputStream output) throws IOException {
        output.write(String.valueOf(value.length).getBytes(StandardCharsets.UTF_8));
        output.write(':');
        output.write(value);
    }

    /**
     * Encode list: l<contents>e
     */
    private static void encodeList(List<?> list, ByteArrayOutputStream output) throws BencodeException, IOException {
        output.write('l');
        for (Object item : list) {
            encode(item, output);
        }
        output.write('e');
    }

    /**
     * Encode dictionary: d<key><value>...e
     * Keys must be sorted in lexicographical order
     */
    private static void encodeDictionary(Map<?, ?> map, ByteArrayOutputStream output) throws BencodeException, IOException {
        output.write('d');

        // Sort keys (BitTorrent spec requires sorted keys)
        TreeMap<String, Object> sortedMap = new TreeMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            sortedMap.put(key, entry.getValue());
        }

        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            encodeString(entry.getKey().getBytes(StandardCharsets.UTF_8), output);
            encode(entry.getValue(), output);
        }

        output.write('e');
    }
}
