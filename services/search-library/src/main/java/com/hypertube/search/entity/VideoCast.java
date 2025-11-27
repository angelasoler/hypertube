package com.hypertube.search.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * VideoCast junction entity linking videos to cast members with role information
 */
@Entity
@Table(name = "video_cast", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"video_id", "cast_member_id", "role", "character_name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoCast {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cast_member_id", nullable = false)
    private CastMember castMember;

    @Column(nullable = false, length = 50)
    private String role; // 'actor', 'director', 'producer', 'writer'

    @Column(name = "character_name")
    private String characterName; // Only for actors

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
