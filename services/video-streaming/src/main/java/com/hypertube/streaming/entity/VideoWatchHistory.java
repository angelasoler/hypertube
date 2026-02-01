package com.hypertube.streaming.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_watch_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoWatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "progress_seconds")
    private Integer progressSeconds;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column
    private Boolean completed;

    @Column(name = "last_watched_at", nullable = false)
    private LocalDateTime lastWatchedAt;

    @PrePersist
    protected void onCreate() {
        if (progressSeconds == null) {
            progressSeconds = 0;
        }
        if (completed == null) {
            completed = false;
        }
        if (lastWatchedAt == null) {
            lastWatchedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastWatchedAt = LocalDateTime.now();
        if (durationSeconds != null && progressSeconds != null) {
            completed = progressSeconds >= (durationSeconds * 0.9); // 90% watched = completed
        }
    }
}
