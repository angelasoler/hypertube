package com.hypertube.search.repository;

import com.hypertube.search.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Video entity operations
 */
@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    /**
     * Find video by IMDb ID
     */
    Optional<Video> findByImdbId(String imdbId);

    /**
     * Check if video exists by IMDb ID
     */
    boolean existsByImdbId(String imdbId);

    /**
     * Search videos by title (case-insensitive partial match)
     */
    @Query("SELECT DISTINCT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Video> searchByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Find videos by genre
     */
    @Query("SELECT DISTINCT v FROM Video v JOIN v.videoGenres vg WHERE vg.genre.name = :genreName")
    Page<Video> findByGenre(@Param("genreName") String genreName, Pageable pageable);

    /**
     * Find videos by year
     */
    Page<Video> findByYear(Integer year, Pageable pageable);

    /**
     * Find videos by year range
     */
    @Query("SELECT v FROM Video v WHERE v.year BETWEEN :startYear AND :endYear")
    Page<Video> findByYearBetween(@Param("startYear") Integer startYear,
                                   @Param("endYear") Integer endYear,
                                   Pageable pageable);

    /**
     * Find videos by minimum IMDb rating
     */
    @Query("SELECT v FROM Video v WHERE v.imdbRating >= :minRating")
    Page<Video> findByMinimumRating(@Param("minRating") BigDecimal minRating, Pageable pageable);

    /**
     * Find popular videos (ordered by sources with most seeds)
     */
    @Query("""
        SELECT DISTINCT v FROM Video v
        LEFT JOIN v.sources s
        GROUP BY v.id
        ORDER BY MAX(s.seeds) DESC, v.createdAt DESC
    """)
    Page<Video> findPopularVideos(Pageable pageable);

    /**
     * Find recently added videos
     */
    Page<Video> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Search videos with filters (title, genre, year range, min rating)
     */
    @Query("""
        SELECT DISTINCT v FROM Video v
        LEFT JOIN v.videoGenres vg
        WHERE (:query IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:genreName IS NULL OR vg.genre.name = :genreName)
        AND (:minYear IS NULL OR v.year >= :minYear)
        AND (:maxYear IS NULL OR v.year <= :maxYear)
        AND (:minRating IS NULL OR v.imdbRating >= :minRating)
    """)
    Page<Video> searchVideos(
        @Param("query") String query,
        @Param("genreName") String genreName,
        @Param("minYear") Integer minYear,
        @Param("maxYear") Integer maxYear,
        @Param("minRating") BigDecimal minRating,
        Pageable pageable
    );

    /**
     * Find videos watched by user
     */
    @Query("""
        SELECT DISTINCT v FROM Video v
        JOIN UserVideoView uvv ON v.id = uvv.video.id
        WHERE uvv.userId = :userId
        ORDER BY uvv.lastWatchedAt DESC
    """)
    Page<Video> findWatchedByUser(@Param("userId") UUID userId, Pageable pageable);
}
