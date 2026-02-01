package com.hypertube.search.repository;

import com.hypertube.search.entity.VideoCast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for VideoCast junction entity operations
 */
@Repository
public interface VideoCastRepository extends JpaRepository<VideoCast, UUID> {

    /**
     * Find all cast for a video
     */
    List<VideoCast> findByVideoIdOrderByDisplayOrder(UUID videoId);

    /**
     * Find cast by role for a video
     */
    @Query("SELECT vc FROM VideoCast vc WHERE vc.video.id = :videoId AND vc.role = :role ORDER BY vc.displayOrder")
    List<VideoCast> findByVideoIdAndRole(@Param("videoId") UUID videoId, @Param("role") String role);

    /**
     * Find all videos for a cast member
     */
    List<VideoCast> findByCastMemberId(UUID castMemberId);
}
