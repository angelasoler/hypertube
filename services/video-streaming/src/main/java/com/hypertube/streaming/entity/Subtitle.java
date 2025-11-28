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
@Table(name = "subtitles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subtitle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode; // en, fr, es, etc.

    @Column(name = "language_name", length = 50)
    private String languageName; // English, French, Spanish

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(nullable = false, length = 10)
    private String format; // srt, vtt, ass

    @Column(length = 50)
    private String source; // opensubtitles, subscene, manual

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (format == null) {
            format = "srt";
        }
    }
}
