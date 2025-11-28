package com.hypertube.search.mapper;

import com.hypertube.search.dto.CastMemberDTO;
import com.hypertube.search.dto.VideoDTO;
import com.hypertube.search.dto.VideoSourceDTO;
import com.hypertube.search.entity.Video;
import com.hypertube.search.entity.VideoCast;
import com.hypertube.search.entity.VideoGenre;
import com.hypertube.search.entity.VideoSource;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper for converting Video entities to DTOs
 */
@Component
public class VideoMapper {

    /**
     * Convert Video entity to DTO
     */
    public VideoDTO toDTO(Video video) {
        return VideoDTO.builder()
            .id(video.getId())
            .imdbId(video.getImdbId())
            .title(video.getTitle())
            .year(video.getYear())
            .runtimeMinutes(video.getRuntimeMinutes())
            .synopsis(video.getSynopsis())
            .imdbRating(video.getImdbRating())
            .posterUrl(video.getPosterUrl())
            .backdropUrl(video.getBackdropUrl())
            .language(video.getLanguage())
            .genres(video.getVideoGenres().stream()
                .map(vg -> vg.getGenre().getName())
                .collect(Collectors.toList()))
            .sources(video.getSources().stream()
                .map(this::toSourceDTO)
                .collect(Collectors.toList()))
            .cast(video.getCast().stream()
                .map(this::toCastDTO)
                .collect(Collectors.toList()))
            .watched(false) // Default, will be set by service if user context available
            .build();
    }

    /**
     * Convert Video entity to DTO (without sources and cast for list views)
     */
    public VideoDTO toLightDTO(Video video) {
        return VideoDTO.builder()
            .id(video.getId())
            .imdbId(video.getImdbId())
            .title(video.getTitle())
            .year(video.getYear())
            .runtimeMinutes(video.getRuntimeMinutes())
            .synopsis(video.getSynopsis())
            .imdbRating(video.getImdbRating())
            .posterUrl(video.getPosterUrl())
            .backdropUrl(video.getBackdropUrl())
            .language(video.getLanguage())
            .genres(video.getVideoGenres().stream()
                .map(vg -> vg.getGenre().getName())
                .collect(Collectors.toList()))
            .watched(false)
            .build();
    }

    /**
     * Convert VideoSource entity to DTO
     */
    private VideoSourceDTO toSourceDTO(VideoSource source) {
        return VideoSourceDTO.builder()
            .id(source.getId())
            .sourceType(source.getSourceType())
            .quality(source.getQuality())
            .torrentHash(source.getTorrentHash())
            .magnetUrl(source.getMagnetUrl())
            .seeds(source.getSeeds())
            .peers(source.getPeers())
            .sizeBytes(source.getSizeBytes())
            .build();
    }

    /**
     * Convert VideoCast entity to DTO
     */
    private CastMemberDTO toCastDTO(VideoCast videoCast) {
        return CastMemberDTO.builder()
            .name(videoCast.getCastMember().getName())
            .role(videoCast.getRole())
            .characterName(videoCast.getCharacterName())
            .profilePhotoUrl(videoCast.getCastMember().getProfilePhotoUrl())
            .build();
    }
}
