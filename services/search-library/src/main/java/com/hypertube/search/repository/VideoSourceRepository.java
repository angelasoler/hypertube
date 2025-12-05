package com.hypertube.search.repository;

import com.hypertube.search.entity.VideoSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VideoSource entity operations
 */
@Repository
public interface VideoSourceRepository extends JpaRepository<VideoSource, UUID> {

    /**
     * Find all sources for a video
     */
    List<VideoSource> findByVideoId(UUID videoId);

    /**
     * Find source by torrent hash
     */
    Optional<VideoSource> findByTorrentHash(String torrentHash);

    /**
     * Find best source for video (highest quality with most seeds)
     */
    @Query("""
        SELECT vs FROM VideoSource vs
        WHERE vs.video.id = :videoId
        ORDER BY vs.quality DESC, vs.seeds DESC
        LIMIT 1
    """)
    Optional<VideoSource> findBestSourceForVideo(@Param("videoId") UUID videoId);

    /**
     * Find sources by video and quality
     */
    Optional<VideoSource> findByVideoIdAndQuality(UUID videoId, String quality);

    /**
     * Find source by video, quality and source type
     */
    @Query("""
        SELECT vs FROM VideoSource vs
        WHERE vs.video = :video
        AND vs.quality = :quality
        AND vs.sourceType = :sourceType
    """)
    Optional<VideoSource> findByVideoAndQualityAndSourceType(
        @Param("video") com.hypertube.search.entity.Video video,
        @Param("quality") String quality,
        @Param("sourceType") String sourceType
    );
}
