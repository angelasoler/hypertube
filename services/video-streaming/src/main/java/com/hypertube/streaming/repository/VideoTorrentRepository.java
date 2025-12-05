package com.hypertube.streaming.repository;

import com.hypertube.streaming.entity.VideoTorrent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoTorrentRepository extends JpaRepository<VideoTorrent, UUID> {

    List<VideoTorrent> findByVideoId(UUID videoId);

    Optional<VideoTorrent> findByVideoIdAndQuality(UUID videoId, String quality);

    @Query("SELECT vt FROM VideoTorrent vt WHERE vt.videoId = :videoId ORDER BY vt.seeds DESC")
    List<VideoTorrent> findByVideoIdOrderBySeedsDesc(@Param("videoId") UUID videoId);

    @Query("SELECT vt FROM VideoTorrent vt WHERE vt.videoId IN :videoIds")
    List<VideoTorrent> findByVideoIdIn(@Param("videoIds") List<UUID> videoIds);

    boolean existsByVideoIdAndQuality(UUID videoId, String quality);
}
