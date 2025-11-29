package com.hypertube.streaming.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing HTTP Range headers according to RFC 7233.
 *
 * Supports:
 * - Single range: "bytes=0-1023"
 * - Open-ended range: "bytes=1024-"
 * - Suffix range: "bytes=-500"
 * - Multiple ranges: "bytes=0-1023,2048-4095"
 */
@Slf4j
public class HttpRangeParser {

    private static final Pattern RANGE_PATTERN = Pattern.compile("^bytes=(.+)$");
    private static final Pattern RANGE_SPEC_PATTERN = Pattern.compile("(\\d*)-(\\d*)");

    /**
     * Represents a byte range with start and end positions.
     */
    @Data
    public static class Range {
        private final long start;
        private final long end;

        public long getLength() {
            return end - start + 1;
        }

        public boolean isValid() {
            return start >= 0 && end >= start;
        }
    }

    /**
     * Parses an HTTP Range header value and returns a list of Range objects.
     *
     * @param rangeHeader The Range header value (e.g., "bytes=0-1023")
     * @param fileSize The total size of the file
     * @return List of Range objects, or empty list if header is invalid
     */
    public static List<Range> parseRangeHeader(String rangeHeader, long fileSize) {
        List<Range> ranges = new ArrayList<>();

        if (rangeHeader == null || rangeHeader.isEmpty()) {
            return ranges;
        }

        Matcher matcher = RANGE_PATTERN.matcher(rangeHeader.trim());
        if (!matcher.matches()) {
            log.warn("Invalid Range header format: {}", rangeHeader);
            return ranges;
        }

        String rangeSpec = matcher.group(1);
        String[] rangeSpecs = rangeSpec.split(",");

        for (String spec : rangeSpecs) {
            Matcher specMatcher = RANGE_SPEC_PATTERN.matcher(spec.trim());
            if (!specMatcher.matches()) {
                log.warn("Invalid range specification: {}", spec);
                continue;
            }

            String startStr = specMatcher.group(1);
            String endStr = specMatcher.group(2);

            long start;
            long end;

            if (startStr.isEmpty() && !endStr.isEmpty()) {
                // Suffix range: bytes=-500 (last 500 bytes)
                long suffixLength = Long.parseLong(endStr);
                start = Math.max(0, fileSize - suffixLength);
                end = fileSize - 1;
            } else if (!startStr.isEmpty() && endStr.isEmpty()) {
                // Open-ended range: bytes=1024- (from 1024 to end)
                start = Long.parseLong(startStr);
                end = fileSize - 1;
            } else if (!startStr.isEmpty() && !endStr.isEmpty()) {
                // Normal range: bytes=0-1023
                start = Long.parseLong(startStr);
                end = Long.parseLong(endStr);
            } else {
                // Invalid: bytes=-
                log.warn("Invalid range specification (both empty): {}", spec);
                continue;
            }

            // Validate range bounds
            if (start < 0 || start >= fileSize) {
                log.warn("Range start {} is out of bounds for file size {}", start, fileSize);
                continue;
            }

            // Adjust end to file size if needed
            end = Math.min(end, fileSize - 1);

            if (end < start) {
                log.warn("Range end {} is before start {} for spec: {}", end, start, spec);
                continue;
            }

            Range range = new Range(start, end);
            ranges.add(range);
            log.debug("Parsed range: {}-{} (length: {})", start, end, range.getLength());
        }

        return ranges;
    }

    /**
     * Checks if a Range header is present and valid.
     *
     * @param rangeHeader The Range header value
     * @return true if the header is present and syntactically valid
     */
    public static boolean isValidRangeHeader(String rangeHeader) {
        if (rangeHeader == null || rangeHeader.isEmpty()) {
            return false;
        }

        return RANGE_PATTERN.matcher(rangeHeader.trim()).matches();
    }

    /**
     * Generates a Content-Range header value for a single range.
     *
     * @param range The range being served
     * @param fileSize The total file size
     * @return Content-Range header value (e.g., "bytes 0-1023/2048")
     */
    public static String generateContentRangeHeader(Range range, long fileSize) {
        return String.format("bytes %d-%d/%d", range.getStart(), range.getEnd(), fileSize);
    }

    /**
     * Checks if a range request is satisfiable for a given file size.
     *
     * @param rangeHeader The Range header value
     * @param fileSize The total file size
     * @return true if at least one range is satisfiable
     */
    public static boolean isSatisfiable(String rangeHeader, long fileSize) {
        List<Range> ranges = parseRangeHeader(rangeHeader, fileSize);
        return !ranges.isEmpty();
    }
}
