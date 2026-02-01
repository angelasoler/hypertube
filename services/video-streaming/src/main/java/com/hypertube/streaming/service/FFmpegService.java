package com.hypertube.streaming.service;

import com.hypertube.streaming.config.StreamingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for video format conversion using FFmpeg.
 *
 * Converts non-browser-compatible formats (MKV, AVI, etc.) to MP4
 * for seamless playback in HTML5 video players.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FFmpegService {

    private final StreamingConfig streamingConfig;

    // Browser-compatible video formats
    private static final Set<String> BROWSER_COMPATIBLE_FORMATS = new HashSet<>(Arrays.asList(
            "mp4", "webm", "ogg"
    ));

    // Non-compatible formats that need conversion
    private static final Set<String> CONVERTIBLE_FORMATS = new HashSet<>(Arrays.asList(
            "mkv", "avi", "mov", "wmv", "flv", "f4v", "m4v", "mpg", "mpeg", "3gp"
    ));

    /**
     * Checks if a video file needs format conversion.
     *
     * @param filePath The path to the video file
     * @return true if conversion is needed, false otherwise
     */
    public boolean needsConversion(String filePath) {
        String extension = getFileExtension(filePath);

        if (extension == null || extension.isEmpty()) {
            log.warn("Unable to determine file extension for: {}", filePath);
            return false;
        }

        extension = extension.toLowerCase();

        // If it's already browser-compatible, no conversion needed
        if (BROWSER_COMPATIBLE_FORMATS.contains(extension)) {
            log.debug("File {} is already browser-compatible ({})", filePath, extension);
            return false;
        }

        // If it's a known convertible format, conversion is needed
        if (CONVERTIBLE_FORMATS.contains(extension)) {
            log.info("File {} needs conversion from {} to MP4", filePath, extension);
            return true;
        }

        // Unknown format - try to convert anyway
        log.warn("Unknown format {} for file {}, will attempt conversion", extension, filePath);
        return true;
    }

    /**
     * Converts a video file to MP4 format using FFmpeg.
     *
     * @param inputPath The input video file path
     * @param outputPath The output MP4 file path
     * @return true if conversion succeeded, false otherwise
     */
    public boolean convertToMp4(String inputPath, String outputPath) {
        try {
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                log.error("Input file not found: {}", inputPath);
                return false;
            }

            File outputFile = new File(outputPath);
            if (outputFile.exists()) {
                log.warn("Output file already exists, will be overwritten: {}", outputPath);
                outputFile.delete();
            }

            // Create output directory if needed
            File outputDir = outputFile.getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
            }

            log.info("Starting FFmpeg conversion: {} -> {}", inputPath, outputPath);

            // Build FFmpeg command
            // -i: input file
            // -c:v libx264: use H.264 video codec
            // -preset medium: balance between speed and quality
            // -crf 23: constant rate factor (quality: 0=lossless, 51=worst, 23=default)
            // -c:a aac: use AAC audio codec
            // -b:a 128k: audio bitrate
            // -movflags +faststart: optimize for web streaming (move metadata to beginning)
            // -y: overwrite output file without asking
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-i", inputPath,
                    "-c:v", "libx264",
                    "-preset", "medium",
                    "-crf", "23",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-movflags", "+faststart",
                    "-y",
                    outputPath
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read and log FFmpeg output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Log progress lines
                    if (line.contains("frame=") || line.contains("time=")) {
                        log.debug("FFmpeg: {}", line.trim());
                    }
                }
            }

            // Wait for conversion to complete (max 1 hour)
            boolean finished = process.waitFor(1, TimeUnit.HOURS);

            if (!finished) {
                log.error("FFmpeg conversion timed out after 1 hour");
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                log.info("FFmpeg conversion completed successfully: {}", outputPath);

                // Verify output file exists and has content
                if (outputFile.exists() && outputFile.length() > 0) {
                    log.info("Output file size: {} bytes", outputFile.length());
                    return true;
                } else {
                    log.error("Output file is missing or empty: {}", outputPath);
                    return false;
                }
            } else {
                log.error("FFmpeg conversion failed with exit code: {}", exitCode);
                log.error("FFmpeg output:\n{}", output.toString());
                return false;
            }

        } catch (IOException e) {
            log.error("IO error during FFmpeg conversion: {}", e.getMessage(), e);
            return false;
        } catch (InterruptedException e) {
            log.error("FFmpeg conversion was interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Gets the duration of a video file in seconds using FFprobe.
     *
     * @param filePath The path to the video file
     * @return The duration in seconds, or -1 if unable to determine
     */
    public long getVideoDuration(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    filePath
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                String durationStr = output.toString().trim();
                return (long) Double.parseDouble(durationStr);
            }

        } catch (Exception e) {
            log.warn("Failed to get video duration for {}: {}", filePath, e.getMessage());
        }

        return -1;
    }

    /**
     * Generates an output file path for the converted video.
     *
     * @param inputPath The input file path
     * @return The output file path with .mp4 extension
     */
    public String generateOutputPath(String inputPath) {
        String basePath = streamingConfig.getStorage().getBasePath();

        File inputFile = new File(inputPath);
        String fileName = inputFile.getName();

        // Remove extension and add .mp4
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        String outputFileName = nameWithoutExt + ".mp4";

        return basePath + File.separator + "converted" + File.separator + outputFileName;
    }

    /**
     * Gets the file extension from a file path.
     *
     * @param filePath The file path
     * @return The file extension (without dot), or null if none
     */
    private String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return null;
        }

        return filePath.substring(lastDotIndex + 1);
    }

    /**
     * Checks if FFmpeg is available on the system.
     *
     * @return true if FFmpeg is available, false otherwise
     */
    public boolean isFFmpegAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }
}
