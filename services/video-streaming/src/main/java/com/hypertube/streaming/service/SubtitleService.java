package com.hypertube.streaming.service;

import com.hypertube.streaming.config.StreamingConfig;
import com.hypertube.streaming.entity.Subtitle;
import com.hypertube.streaming.repository.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for downloading and managing video subtitles.
 *
 * Supports:
 * - Automatic English subtitle downloads
 * - User preferred language subtitle downloads
 * - SRT to WebVTT conversion for browser compatibility
 * - Subtitle caching and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubtitleService {

    private final SubtitleRepository subtitleRepository;
    private final StreamingConfig streamingConfig;

    private static final Pattern SRT_TIMESTAMP_PATTERN = Pattern.compile(
            "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})"
    );

    /**
     * Downloads subtitles for a video.
     *
     * NOTE: This is a placeholder implementation. Full implementation would integrate with
     * OpenSubtitles API (https://www.opensubtitles.org/api).
     *
     * @param videoId The video ID
     * @param imdbId The IMDB ID (optional)
     * @param languages List of language codes to download (e.g., "en", "pt", "es")
     * @return List of downloaded subtitle entities
     */
    public List<Subtitle> downloadSubtitles(UUID videoId, String imdbId, List<String> languages) {
        List<Subtitle> subtitles = new ArrayList<>();

        log.info("PLACEHOLDER: Subtitle download requested for video: {}, languages: {}",
                videoId, languages);
        log.warn("Subtitle download requires OpenSubtitles API integration");
        log.info("Required API: https://www.opensubtitles.org/api");

        // Placeholder: In real implementation:
        // 1. Authenticate with OpenSubtitles API
        // 2. Search for subtitles by IMDB ID or video hash
        // 3. Download subtitle files (.srt format)
        // 4. Convert to WebVTT format
        // 5. Save to disk and database

        return subtitles;
    }

    /**
     * Converts an SRT subtitle file to WebVTT format for browser compatibility.
     *
     * @param srtFilePath Path to the SRT file
     * @param vttFilePath Path where the VTT file will be saved
     * @return true if conversion succeeded, false otherwise
     */
    public boolean convertSrtToVtt(String srtFilePath, String vttFilePath) {
        try {
            File srtFile = new File(srtFilePath);
            if (!srtFile.exists()) {
                log.error("SRT file not found: {}", srtFilePath);
                return false;
            }

            log.info("Converting SRT to VTT: {} -> {}", srtFilePath, vttFilePath);

            List<String> srtLines = Files.readAllLines(Paths.get(srtFilePath), StandardCharsets.UTF_8);
            List<String> vttLines = new ArrayList<>();

            // Add WebVTT header
            vttLines.add("WEBVTT");
            vttLines.add("");

            // Convert SRT format to VTT
            for (String line : srtLines) {
                // Convert timestamp format: 00:00:00,000 --> 00:00:00.000
                Matcher matcher = SRT_TIMESTAMP_PATTERN.matcher(line);
                if (matcher.find()) {
                    String startHour = matcher.group(1);
                    String startMin = matcher.group(2);
                    String startSec = matcher.group(3);
                    String startMs = matcher.group(4);
                    String endHour = matcher.group(5);
                    String endMin = matcher.group(6);
                    String endSec = matcher.group(7);
                    String endMs = matcher.group(8);

                    String vttTimestamp = String.format("%s:%s:%s.%s --> %s:%s:%s.%s",
                            startHour, startMin, startSec, startMs,
                            endHour, endMin, endSec, endMs);

                    vttLines.add(vttTimestamp);
                } else {
                    vttLines.add(line);
                }
            }

            // Write VTT file
            Path vttPath = Paths.get(vttFilePath);
            Files.createDirectories(vttPath.getParent());
            Files.write(vttPath, vttLines, StandardCharsets.UTF_8);

            log.info("Successfully converted SRT to VTT: {}", vttFilePath);
            return true;

        } catch (IOException e) {
            log.error("Error converting SRT to VTT: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Creates a subtitle entity and saves it to the database.
     *
     * @param videoId The video ID
     * @param languageCode The language code (e.g., "en", "pt")
     * @param filePath The path to the subtitle file
     * @param format The subtitle format ("srt" or "vtt")
     * @return The created Subtitle entity
     */
    public Subtitle createSubtitle(UUID videoId, String languageCode, String filePath, String format) {
        Subtitle subtitle = new Subtitle();
        subtitle.setVideoId(videoId);
        subtitle.setLanguageCode(languageCode);
        subtitle.setFilePath(filePath);
        subtitle.setFormat(format);

        subtitle = subtitleRepository.save(subtitle);
        log.info("Created subtitle entry: {} (language: {}, format: {})",
                subtitle.getId(), languageCode, format);

        return subtitle;
    }

    /**
     * Gets all subtitles for a video.
     *
     * @param videoId The video ID
     * @return List of subtitles
     */
    public List<Subtitle> getSubtitlesForVideo(UUID videoId) {
        return subtitleRepository.findByVideoId(videoId);
    }

    /**
     * Gets a specific subtitle by language.
     *
     * @param videoId The video ID
     * @param languageCode The language code
     * @return The subtitle, or null if not found
     */
    public Subtitle getSubtitleByLanguage(UUID videoId, String languageCode) {
        return subtitleRepository.findByVideoIdAndLanguageCode(videoId, languageCode)
                .orElse(null);
    }

    /**
     * Generates a file path for a subtitle file.
     *
     * @param videoId The video ID
     * @param languageCode The language code
     * @param format The format ("srt" or "vtt")
     * @return The generated file path
     */
    public String generateSubtitlePath(UUID videoId, String languageCode, String format) {
        String basePath = streamingConfig.getStorage().getBasePath();
        return String.format("%s/subtitles/%s/%s.%s",
                basePath, videoId, languageCode, format);
    }

    /**
     * Validates if a subtitle file exists and is readable.
     *
     * @param filePath The file path
     * @return true if valid, false otherwise
     */
    public boolean isValidSubtitleFile(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists() && file.canRead() && file.length() > 0;
        } catch (Exception e) {
            log.warn("Subtitle file validation failed for {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    /**
     * Deletes old subtitle files for a video.
     *
     * @param videoId The video ID
     */
    public void deleteSubtitlesForVideo(UUID videoId) {
        List<Subtitle> subtitles = subtitleRepository.findByVideoId(videoId);

        for (Subtitle subtitle : subtitles) {
            try {
                // Delete file
                File file = new File(subtitle.getFilePath());
                if (file.exists()) {
                    file.delete();
                    log.info("Deleted subtitle file: {}", subtitle.getFilePath());
                }

                // Delete database entry
                subtitleRepository.delete(subtitle);
                log.info("Deleted subtitle entry: {}", subtitle.getId());

            } catch (Exception e) {
                log.error("Error deleting subtitle {}: {}", subtitle.getId(), e.getMessage());
            }
        }
    }
}
