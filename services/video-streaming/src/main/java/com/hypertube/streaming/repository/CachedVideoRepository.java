package com.hypertube.streaming.repository;

import com.hypertube.streaming.entity.CachedVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CachedVideoRepository extends JpaRepository<CachedVideo, UUID> {

    Optional<CachedVideo> findByVideoIdAndTorrentId(UUID videoId, UUID torrentId);

    Optional<CachedVideo> findByVideoId(UUID videoId);

    List<CachedVideo> findByExpiresAtBefore(LocalDateTime dateTime);

    @Query("SELECT cv FROM CachedVideo cv WHERE cv.expiresAt < :now ORDER BY cv.lastAccessedAt ASC")
    List<CachedVideo> findExpiredVideos(@Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(cv.fileSize), 0) FROM CachedVideo cv")
    long getTotalCacheSize();

    @Modifying
    @Query("UPDATE CachedVideo cv SET cv.lastAccessedAt = :accessTime, cv.accessCount = cv.accessCount + 1 " +
           "WHERE cv.id = :id")
    void updateAccessInfo(@Param("id") UUID id, @Param("accessTime") LocalDateTime accessTime);

    @Query("SELECT cv FROM CachedVideo cv ORDER BY cv.lastAccessedAt ASC")
    List<CachedVideo> findLeastRecentlyAccessed();

    boolean existsByVideoIdAndTorrentId(UUID videoId, UUID torrentId);
}
