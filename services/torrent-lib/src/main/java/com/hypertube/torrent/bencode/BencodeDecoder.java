package com.hypertube.torrent.bencode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bencode decoder for BitTorrent protocol
 *
 * Bencode Format:
 * - Integers: i<number>e (e.g., i42e)
 * - Strings: <length>:<string> (e.g., 4:spam)
 * - Lists: l<contents>e (e.g., l4:spam4:eggse)
 * - Dictionaries: d<key><value>...e (e.g., d3:cow3:moo4:spam4:eggse)
 */
public class BencodeDecoder {

    /**
     * Decode bencode data from byte array
     */
    public static Object decode(byte[] data) throws BencodeException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            return decode(input);
        } catch (IOException e) {
            throw new BencodeException("Failed to decode bencode data", e);
        }
    }

    /**
     * Decode bencode data from input stream
     */
    public static Object decode(ByteArrayInputStream input) throws BencodeException, IOException {
        int next = input.read();
        if (next == -1) {
            throw new BencodeException("Unexpected end of stream");
        }

        return switch (next) {
            case 'i' -> decodeInteger(input);
            case 'l' -> decodeList(input);
            case 'd' -> decodeDictionary(input);
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                // Push back the digit and decode string
                input.reset();
                input.mark(1);
                input.read(); // consume the mark
                yield decodeString(input, next - '0');
            }
            default -> throw new BencodeException("Invalid bencode data: unexpected character '" + (char) next + "'");
        };
    }

    /**
     * Decode integer: i<number>e
     */
    private static Long decodeInteger(ByteArrayInputStream input) throws BencodeException, IOException {
        StringBuilder sb = new StringBuilder();
        int ch;

        while ((ch = input.read()) != 'e') {
            if (ch == -1) {
                throw new BencodeException("Unexpected end of stream while decoding integer");
            }
            sb.append((char) ch);
        }

        try {
            return Long.parseLong(sb.toString());
        } catch (NumberFormatException e) {
            throw new BencodeException("Invalid integer: " + sb, e);
        }
    }

    /**
     * Decode string: <length>:<string>
     */
    private static byte[] decodeString(ByteArrayInputStream input, int firstDigit) throws BencodeException, IOException {
        // Read length
        StringBuilder lengthStr = new StringBuilder();
        lengthStr.append(firstDigit);

        int ch;
        while ((ch = input.read()) != ':') {
            if (ch == -1) {
                throw new BencodeException("Unexpected end of stream while decoding string length");
            }
            if (ch < '0' || ch > '9') {
                throw new BencodeException("Invalid string length character: " + (char) ch);
            }
            lengthStr.append((char) ch);
        }

        int length;
        try {
            length = Integer.parseInt(lengthStr.toString());
        } catch (NumberFormatException e) {
            throw new BencodeException("Invalid string length: " + lengthStr, e);
        }

        // Read string bytes
        byte[] bytes = new byte[length];
        int read = input.read(bytes);
        if (read != length) {
            throw new BencodeException("Unexpected end of stream while reading string data");
        }

        return bytes;
    }

    /**
     * Decode list: l<contents>e
     */
    private static List<Object> decodeList(ByteArrayInputStream input) throws BencodeException, IOException {
        List<Object> list = new ArrayList<>();
        input.mark(1);
        int next = input.read();

        while (next != 'e') {
            if (next == -1) {
                throw new BencodeException("Unexpected end of stream while decoding list");
            }
            input.reset();
            list.add(decode(input));
            input.mark(1);
            next = input.read();
        }

        return list;
    }

    /**
     * Decode dictionary: d<key><value>...e
     */
    private static Map<String, Object> decodeDictionary(ByteArrayInputStream input) throws BencodeException, IOException {
        Map<String, Object> dict = new LinkedHashMap<>();
        input.mark(1);
        int next = input.read();

        while (next != 'e') {
            if (next == -1) {
                throw new BencodeException("Unexpected end of stream while decoding dictionary");
            }

            // Keys must be strings
            input.reset();
            Object keyObj = decode(input);
            if (!(keyObj instanceof byte[])) {
                throw new BencodeException("Dictionary keys must be strings");
            }

            String key = new String((byte[]) keyObj, StandardCharsets.UTF_8);
            Object value = decode(input);
            dict.put(key, value);

            input.mark(1);
            next = input.read();
        }

        return dict;
    }

    /**
     * Helper method to convert byte[] values to strings in decoded data
     */
    public static String bytesToString(Object obj) {
        if (obj instanceof byte[]) {
            return new String((byte[]) obj, StandardCharsets.UTF_8);
        }
        return obj != null ? obj.toString() : null;
    }

    /**
     * Helper method to get string from dictionary
     */
    @SuppressWarnings("unchecked")
    public static String getString(Map<String, Object> dict, String key) {
        Object value = dict.get(key);
        return value instanceof byte[] ? new String((byte[]) value, StandardCharsets.UTF_8) : null;
    }

    /**
     * Helper method to get long from dictionary
     */
    @SuppressWarnings("unchecked")
    public static Long getLong(Map<String, Object> dict, String key) {
        Object value = dict.get(key);
        return value instanceof Long ? (Long) value : null;
    }

    /**
     * Helper method to get list from dictionary
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> dict, String key) {
        Object value = dict.get(key);
        return value instanceof List ? (List<Object>) value : null;
    }

    /**
     * Helper method to get map from dictionary
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> dict, String key) {
        Object value = dict.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
}
