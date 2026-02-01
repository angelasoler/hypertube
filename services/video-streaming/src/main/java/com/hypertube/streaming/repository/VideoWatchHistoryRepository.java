package com.hypertube.streaming.repository;

import com.hypertube.streaming.entity.VideoWatchHistory;
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
public interface VideoWatchHistoryRepository extends JpaRepository<VideoWatchHistory, UUID> {

    Optional<VideoWatchHistory> findByUserIdAndVideoId(UUID userId, UUID videoId);

    List<VideoWatchHistory> findByUserId(UUID userId);

    List<VideoWatchHistory> findByUserIdOrderByLastWatchedAtDesc(UUID userId);

    @Query("SELECT vwh FROM VideoWatchHistory vwh WHERE vwh.userId = :userId AND vwh.completed = true " +
           "ORDER BY vwh.lastWatchedAt DESC")
    List<VideoWatchHistory> findCompletedByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE VideoWatchHistory vwh SET vwh.progressSeconds = :progressSeconds, " +
           "vwh.lastWatchedAt = :lastWatchedAt WHERE vwh.userId = :userId AND vwh.videoId = :videoId")
    int updateProgress(@Param("userId") UUID userId,
                      @Param("videoId") UUID videoId,
                      @Param("progressSeconds") Integer progressSeconds,
                      @Param("lastWatchedAt") LocalDateTime lastWatchedAt);
}
