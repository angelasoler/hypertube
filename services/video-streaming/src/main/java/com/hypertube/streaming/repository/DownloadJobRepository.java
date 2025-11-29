package com.hypertube.streaming.repository;

import com.hypertube.streaming.entity.DownloadJob;
import com.hypertube.streaming.entity.DownloadJob.DownloadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DownloadJobRepository extends JpaRepository<DownloadJob, UUID> {

    List<DownloadJob> findByStatus(DownloadStatus status);

    List<DownloadJob> findByUserId(UUID userId);

    List<DownloadJob> findByVideoId(UUID videoId);

    Optional<DownloadJob> findByVideoIdAndUserId(UUID videoId, UUID userId);

    @Query("SELECT dj FROM DownloadJob dj WHERE dj.status IN :statuses ORDER BY dj.createdAt ASC")
    List<DownloadJob> findByStatusIn(@Param("statuses") List<DownloadStatus> statuses);

    @Query("SELECT dj FROM DownloadJob dj WHERE dj.status = :status AND dj.createdAt < :before")
    List<DownloadJob> findStaleJobs(@Param("status") DownloadStatus status, @Param("before") LocalDateTime before);

    @Query("SELECT CASE WHEN COUNT(dj) > 0 THEN true ELSE false END FROM DownloadJob dj " +
           "WHERE dj.videoId = :videoId AND dj.status IN :activeStatuses")
    boolean hasActiveDownload(@Param("videoId") UUID videoId,
                             @Param("activeStatuses") List<DownloadStatus> activeStatuses);

    @Query("SELECT COUNT(dj) FROM DownloadJob dj WHERE dj.status IN :activeStatuses")
    long countActiveJobs(@Param("activeStatuses") List<DownloadStatus> activeStatuses);
}
