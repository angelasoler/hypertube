package com.hypertube.streaming.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "streaming")
@Data
public class StreamingConfig {

    private Storage storage = new Storage();
    private Torrent torrent = new Torrent();
    private Conversion conversion = new Conversion();
    private Cache cache = new Cache();
    private Subtitle subtitle = new Subtitle();

    @Data
    public static class Storage {
        private String basePath = "/tmp/hypertube/videos";
        private String tempPath = "/tmp/hypertube/temp";
        private String subtitlePath = "/tmp/hypertube/subtitles";
        private int maxCacheSizeGb = 100;
    }

    @Data
    public static class Torrent {
        private String downloadPath = "/tmp/hypertube/downloads";
        private int maxDownloadSpeed = -1; // -1 for unlimited
        private int maxUploadSpeed = 100; // KB/s
        private int maxConnections = 200;
        private boolean dhtEnabled = true;
        private int portRangeStart = 6881;
        private int portRangeEnd = 6889;
    }

    @Data
    public static class Conversion {
        private boolean enabled = true;
        private String targetFormat = "mp4";
        private String targetCodec = "h264";
        private String ffmpegPath = "ffmpeg";
        private String ffprobePath = "ffprobe";
    }

    @Data
    public static class Cache {
        private int ttlDays = 30;
        private int cleanupIntervalHours = 6;
    }

    @Data
    public static class Subtitle {
        private boolean enabled = true;
        private String autoDownloadLanguages = "en";
        private String opensubtitlesApiKey;
    }
}
