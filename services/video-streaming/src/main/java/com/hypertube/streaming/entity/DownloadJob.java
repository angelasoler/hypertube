package com.hypertube.streaming.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "download_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "torrent_id", nullable = false)
    private UUID torrentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DownloadStatus status;

    @Column(nullable = false)
    private Integer progress; // 0-100

    @Column(name = "download_speed")
    private Long downloadSpeed; // bytes per second

    @Column(name = "eta_seconds")
    private Integer etaSeconds; // estimated time to completion

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum DownloadStatus {
        PENDING,
        DOWNLOADING,
        CONVERTING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
