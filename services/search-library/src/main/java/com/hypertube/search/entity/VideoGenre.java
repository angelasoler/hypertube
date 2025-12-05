package com.hypertube.search.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Junction entity for Video-Genre many-to-many relationship
 */
@Entity
@Table(name = "video_genres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(VideoGenreId.class)
public class VideoGenre {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;
}
