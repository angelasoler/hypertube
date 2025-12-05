package com.hypertube.torrent.bencode;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class BencodeEncoderTest {

    @Test
    void testEncodeInteger() throws BencodeException {
        byte[] result = BencodeEncoder.encode(42L);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("i42e");
    }

    @Test
    void testEncodeNegativeInteger() throws BencodeException {
        byte[] result = BencodeEncoder.encode(-42L);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("i-42e");
    }

    @Test
    void testEncodeZero() throws BencodeException {
        byte[] result = BencodeEncoder.encode(0L);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("i0e");
    }

    @Test
    void testEncodeString() throws BencodeException {
        byte[] result = BencodeEncoder.encode("spam");
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("4:spam");
    }

    @Test
    void testEncodeEmptyString() throws BencodeException {
        byte[] result = BencodeEncoder.encode("");
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("0:");
    }

    @Test
    void testEncodeByteArray() throws BencodeException {
        byte[] input = "spam".getBytes(StandardCharsets.UTF_8);
        byte[] result = BencodeEncoder.encode(input);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("4:spam");
    }

    @Test
    void testEncodeList() throws BencodeException {
        List<String> list = Arrays.asList("spam", "eggs");
        byte[] result = BencodeEncoder.encode(list);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("l4:spam4:eggse");
    }

    @Test
    void testEncodeEmptyList() throws BencodeException {
        List<String> list = Arrays.asList();
        byte[] result = BencodeEncoder.encode(list);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("le");
    }

    @Test
    void testEncodeNestedList() throws BencodeException {
        List<Object> innerList = Arrays.asList("spam");
        List<Object> outerList = Arrays.asList(innerList);
        byte[] result = BencodeEncoder.encode(outerList);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("ll4:spamee");
    }

    @Test
    void testEncodeDictionary() throws BencodeException {
        Map<String, String> dict = new HashMap<>();
        dict.put("cow", "moo");
        dict.put("spam", "eggs");

        byte[] result = BencodeEncoder.encode(dict);
        String encoded = new String(result, StandardCharsets.UTF_8);

        // Keys should be sorted
        assertThat(encoded).isEqualTo("d3:cow3:moo4:spam4:eggse");
    }

    @Test
    void testEncodeEmptyDictionary() throws BencodeException {
        Map<String, String> dict = new HashMap<>();
        byte[] result = BencodeEncoder.encode(dict);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("de");
    }

    @Test
    void testEncodeComplexStructure() throws BencodeException {
        Map<String, Object> dict = new HashMap<>();
        dict.put("name", "John");
        dict.put("age", 30L);
        dict.put("hobbies", Arrays.asList("coding", "reading"));

        byte[] result = BencodeEncoder.encode(dict);
        String encoded = new String(result, StandardCharsets.UTF_8);

        // Verify it can be decoded back
        Object decoded = BencodeDecoder.decode(result);
        assertThat(decoded).isInstanceOf(Map.class);

        Map<String, Object> decodedDict = (Map<String, Object>) decoded;
        assertThat(BencodeDecoder.getString(decodedDict, "name")).isEqualTo("John");
        assertThat(BencodeDecoder.getLong(decodedDict, "age")).isEqualTo(30L);
    }

    @Test
    void testDictionaryKeysSorted() throws BencodeException {
        Map<String, String> dict = new HashMap<>();
        dict.put("zebra", "z");
        dict.put("apple", "a");
        dict.put("banana", "b");

        byte[] result = BencodeEncoder.encode(dict);
        String encoded = new String(result, StandardCharsets.UTF_8);

        // Keys should be sorted alphabetically
        assertThat(encoded).isEqualTo("d5:apple1:a6:banana1:b5:zebra1:ze");
    }

    @Test
    void testEncodeDecodeRoundTrip() throws BencodeException {
        Map<String, Object> original = new HashMap<>();
        original.put("string", "value");
        original.put("number", 42L);
        original.put("list", Arrays.asList("a", "b", "c"));

        byte[] encoded = BencodeEncoder.encode(original);
        Object decoded = BencodeDecoder.decode(encoded);

        assertThat(decoded).isInstanceOf(Map.class);
        Map<String, Object> decodedMap = (Map<String, Object>) decoded;

        assertThat(BencodeDecoder.getString(decodedMap, "string")).isEqualTo("value");
        assertThat(BencodeDecoder.getLong(decodedMap, "number")).isEqualTo(42L);
        assertThat(BencodeDecoder.getList(decodedMap, "list")).hasSize(3);
    }

    @Test
    void testUnsupportedType() {
        Object unsupported = new Object();

        assertThatThrownBy(() -> BencodeEncoder.encode(unsupported))
            .isInstanceOf(BencodeException.class)
            .hasMessageContaining("Unsupported type");
    }
}
