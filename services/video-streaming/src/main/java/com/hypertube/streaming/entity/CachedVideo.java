package com.hypertube.streaming.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cached_videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CachedVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "torrent_id", nullable = false)
    private UUID torrentId;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 20)
    private String format; // mp4, webm, mkv

    @Column(length = 50)
    private String codec; // h264, h265, vp9

    @Column(length = 20)
    private String resolution; // 1920x1080, 1280x720

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column
    private Integer bitrate; // kbps

    @CreationTimestamp
    @Column(name = "cached_at", nullable = false, updatable = false)
    private LocalDateTime cachedAt;

    @Column(name = "last_accessed_at", nullable = false)
    private LocalDateTime lastAccessedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "access_count")
    private Integer accessCount;

    @PrePersist
    protected void onCreate() {
        if (lastAccessedAt == null) {
            lastAccessedAt = LocalDateTime.now();
        }
        if (accessCount == null) {
            accessCount = 0;
        }
    }
}
