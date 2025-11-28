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
@Table(name = "video_torrents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoTorrent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(nullable = false, length = 20)
    private String quality; // 720p, 1080p, 2160p

    @Column(name = "torrent_url", length = 1000)
    private String torrentUrl;

    @Column(name = "magnet_link", columnDefinition = "TEXT")
    private String magnetLink;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column
    private Integer seeds;

    @Column
    private Integer peers;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
