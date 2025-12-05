package com.hypertube.search.repository;

import com.hypertube.search.entity.UserVideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserVideoView entity operations
 */
@Repository
public interface UserVideoViewRepository extends JpaRepository<UserVideoView, UUID> {

    /**
     * Find view record by user and video
     */
    Optional<UserVideoView> findByUserIdAndVideoId(UUID userId, UUID videoId);

    /**
     * Check if user has watched video
     */
    boolean existsByUserIdAndVideoId(UUID userId, UUID videoId);

    /**
     * Update last watched timestamp for existing view or create new
     */
    @Modifying
    @Query("""
        UPDATE UserVideoView uvv
        SET uvv.lastWatchedAt = :timestamp
        WHERE uvv.userId = :userId AND uvv.video.id = :videoId
    """)
    void updateLastWatchedAt(@Param("userId") UUID userId,
                             @Param("videoId") UUID videoId,
                             @Param("timestamp") Instant timestamp);
}
