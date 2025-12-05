package com.hypertube.torrent.bencode;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class BencodeDecoderTest {

    @Test
    void testDecodeInteger() throws BencodeException {
        byte[] data = "i42e".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isInstanceOf(Long.class);
        assertThat(result).isEqualTo(42L);
    }

    @Test
    void testDecodeNegativeInteger() throws BencodeException {
        byte[] data = "i-42e".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isEqualTo(-42L);
    }

    @Test
    void testDecodeZero() throws BencodeException {
        byte[] data = "i0e".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    void testDecodeString() throws BencodeException {
        byte[] data = "4:spam".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isInstanceOf(byte[].class);
        assertThat(new String((byte[]) result, StandardCharsets.UTF_8)).isEqualTo("spam");
    }

    @Test
    void testDecodeEmptyString() throws BencodeException {
        byte[] data = "0:".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isInstanceOf(byte[].class);
        assertThat(((byte[]) result).length).isEqualTo(0);
    }

    @Test
    void testDecodeList() throws BencodeException {
        byte[] data = "l4:spam4:eggse".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isInstanceOf(List.class);
        List<?> list = (List<?>) result;
        assertThat(list).hasSize(2);
        assertThat(new String((byte[]) list.get(0), StandardCharsets.UTF_8)).isEqualTo("spam");
        assertThat(new String((byte[]) list.get(1), StandardCharsets.UTF_8)).isEqualTo("eggs");
    }

    @Test
    void testDecodeEmptyList() throws BencodeException {
        byte[] data = "le".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isInstanceOf(List.class);
        assertThat((List<?>) result).isEmpty();
    }

    @Test
    void testDecodeNestedList() throws BencodeException {
        byte[] data = "ll4:spamee".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isInstanceOf(List.class);
        List<?> outerList = (List<?>) result;
        assertThat(outerList).hasSize(1);

        List<?> innerList = (List<?>) outerList.get(0);
        assertThat(innerList).hasSize(1);
        assertThat(new String((byte[]) innerList.get(0), StandardCharsets.UTF_8)).isEqualTo("spam");
    }

    @Test
    void testDecodeDictionary() throws BencodeException {
        byte[] data = "d3:cow3:moo4:spam4:eggse".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isInstanceOf(Map.class);
        Map<?, ?> dict = (Map<?, ?>) result;
        assertThat(dict).hasSize(2);
        assertThat(BencodeDecoder.getString((Map<String, Object>) dict, "cow")).isEqualTo("moo");
        assertThat(BencodeDecoder.getString((Map<String, Object>) dict, "spam")).isEqualTo("eggs");
    }

    @Test
    void testDecodeEmptyDictionary() throws BencodeException {
        byte[] data = "de".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isInstanceOf(Map.class);
        assertThat((Map<?, ?>) result).isEmpty();
    }

    @Test
    void testDecodeComplexStructure() throws BencodeException {
        // d4:name4:John3:agei30e6:hobbisl6:coding7:readingee
        // {"name": "John", "age": 30, "hobbies": ["coding", "reading"]}
        byte[] data = "d4:name4:John3:agei30e7:hobbiesl6:coding7:readingee".getBytes(StandardCharsets.UTF_8);
        Object result = BencodeDecoder.decode(data);

        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> dict = (Map<String, Object>) result;

        assertThat(BencodeDecoder.getString(dict, "name")).isEqualTo("John");
        assertThat(BencodeDecoder.getLong(dict, "age")).isEqualTo(30L);

        List<Object> hobbies = BencodeDecoder.getList(dict, "hobbies");
        assertThat(hobbies).hasSize(2);
        assertThat(new String((byte[]) hobbies.get(0), StandardCharsets.UTF_8)).isEqualTo("coding");
        assertThat(new String((byte[]) hobbies.get(1), StandardCharsets.UTF_8)).isEqualTo("reading");
    }

    @Test
    void testInvalidData() {
        byte[] data = "x42e".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> BencodeDecoder.decode(data))
            .isInstanceOf(BencodeException.class)
            .hasMessageContaining("Invalid bencode data");
    }

    @Test
    void testUnterminatedInteger() {
        byte[] data = "i42".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> BencodeDecoder.decode(data))
            .isInstanceOf(BencodeException.class)
            .hasMessageContaining("Unexpected end of stream");
    }

    @Test
    void testInvalidStringLength() {
        byte[] data = "abc:spam".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> BencodeDecoder.decode(data))
            .isInstanceOf(BencodeException.class);
    }

    @Test
    void testHelperMethods() throws BencodeException {
        byte[] data = "d3:key5:valuee".getBytes(StandardCharsets.UTF_8);
        Map<String, Object> dict = (Map<String, Object>) BencodeDecoder.decode(data);

        assertThat(BencodeDecoder.getString(dict, "key")).isEqualTo("value");
        assertThat(BencodeDecoder.getString(dict, "nonexistent")).isNull();
    }
}
