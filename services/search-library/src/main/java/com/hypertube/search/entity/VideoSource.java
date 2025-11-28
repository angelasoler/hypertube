package com.hypertube.search.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * VideoSource entity representing a torrent source for a video
 */
@Entity
@Table(name = "video_sources", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"video_id", "source_type", "quality"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType; // 'yts', 'eztv', 'custom'

    @Column(nullable = false, length = 20)
    private String quality; // '720p', '1080p', '2160p', etc.

    @Column(name = "torrent_hash", nullable = false, length = 40)
    private String torrentHash;

    @Column(name = "magnet_url", nullable = false, columnDefinition = "TEXT")
    private String magnetUrl;

    @Builder.Default
    private Integer seeds = 0;

    @Builder.Default
    private Integer peers = 0;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
