package com.hypertube.streaming.repository;

import com.hypertube.streaming.entity.Subtitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubtitleRepository extends JpaRepository<Subtitle, UUID> {

    List<Subtitle> findByVideoId(UUID videoId);

    Optional<Subtitle> findByVideoIdAndLanguageCode(UUID videoId, String languageCode);

    @Query("SELECT s FROM Subtitle s WHERE s.videoId = :videoId AND s.languageCode IN :languageCodes")
    List<Subtitle> findByVideoIdAndLanguageCodes(@Param("videoId") UUID videoId,
                                                  @Param("languageCodes") List<String> languageCodes);

    boolean existsByVideoIdAndLanguageCode(UUID videoId, String languageCode);

    @Query("SELECT COUNT(s) FROM Subtitle s WHERE s.videoId = :videoId")
    long countByVideoId(@Param("videoId") UUID videoId);
}
