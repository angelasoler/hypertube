package com.hypertube.search.service;

import com.hypertube.search.client.TmdbClient;
import com.hypertube.search.client.YtsClient;
import com.hypertube.search.dto.PagedResponse;
import com.hypertube.search.dto.SearchRequest;
import com.hypertube.search.dto.VideoDTO;
import com.hypertube.search.entity.*;
import com.hypertube.search.mapper.VideoMapper;
import com.hypertube.search.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for video search, aggregation, and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final GenreRepository genreRepository;
    private final VideoSourceRepository videoSourceRepository;
    private final CastMemberRepository castMemberRepository;
    private final VideoCastRepository videoCastRepository;
    private final UserVideoViewRepository userVideoViewRepository;
    private final VideoMapper videoMapper;
    private final YtsClient ytsClient;
    private final TmdbClient tmdbClient;

    /**
     * Search videos with filters and pagination
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "videoSearch", key = "#request.toString()")
    public PagedResponse<VideoDTO> searchVideos(SearchRequest request, UUID userId) {
        Pageable pageable = createPageable(request);

        Page<Video> videoPage = videoRepository.searchVideos(
            request.getQuery(),
            request.getGenre(),
            request.getMinYear(),
            request.getMaxYear(),
            request.getMinRating(),
            pageable
        );

        List<VideoDTO> videoDTOs = videoPage.getContent().stream()
            .map(videoMapper::toLightDTO)
            .collect(Collectors.toList());

        // Mark watched videos if user context available
        if (userId != null) {
            markWatchedVideos(videoDTOs, userId);
        }

        return PagedResponse.<VideoDTO>builder()
            .content(videoDTOs)
            .page(videoPage.getNumber())
            .size(videoPage.getSize())
            .totalElements(videoPage.getTotalElements())
            .totalPages(videoPage.getTotalPages())
            .last(videoPage.isLast())
            .build();
    }

    /**
     * Get popular videos
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "popularVideos", key = "'page:' + #page + ':size:' + #size")
    public PagedResponse<VideoDTO> getPopularVideos(int page, int size, UUID userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videoPage = videoRepository.findPopularVideos(pageable);

        List<VideoDTO> videoDTOs = videoPage.getContent().stream()
            .map(videoMapper::toLightDTO)
            .collect(Collectors.toList());

        if (userId != null) {
            markWatchedVideos(videoDTOs, userId);
        }

        return PagedResponse.<VideoDTO>builder()
            .content(videoDTOs)
            .page(videoPage.getNumber())
            .size(videoPage.getSize())
            .totalElements(videoPage.getTotalElements())
            .totalPages(videoPage.getTotalPages())
            .last(videoPage.isLast())
            .build();
    }

    /**
     * Get video details by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "videoDetails", key = "'video:' + #videoId")
    public VideoDTO getVideoById(UUID videoId, UUID userId) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        VideoDTO dto = videoMapper.toDTO(video);

        if (userId != null && userVideoViewRepository.existsByUserIdAndVideoId(userId, videoId)) {
            dto.setWatched(true);
        }

        return dto;
    }

    /**
     * Get video details by IMDb ID
     */
    @Transactional(readOnly = true)
    public VideoDTO getVideoByImdbId(String imdbId, UUID userId) {
        Video video = videoRepository.findByImdbId(imdbId)
            .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        VideoDTO dto = videoMapper.toDTO(video);

        if (userId != null && userVideoViewRepository.existsByUserIdAndVideoId(userId, video.getId())) {
            dto.setWatched(true);
        }

        return dto;
    }

    /**
     * Mark video as watched by user
     */
    @Transactional
    public void markAsWatched(UUID videoId, UUID userId) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        Optional<UserVideoView> existing = userVideoViewRepository.findByUserIdAndVideoId(userId, videoId);

        if (existing.isPresent()) {
            userVideoViewRepository.updateLastWatchedAt(userId, videoId, Instant.now());
        } else {
            UserVideoView view = UserVideoView.builder()
                .userId(userId)
                .video(video)
                .lastWatchedAt(Instant.now())
                .build();
            userVideoViewRepository.save(view);
        }
    }

    /**
     * Get user's watched videos
     */
    @Transactional(readOnly = true)
    public PagedResponse<VideoDTO> getWatchedVideos(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videoPage = videoRepository.findWatchedByUser(userId, pageable);

        List<VideoDTO> videoDTOs = videoPage.getContent().stream()
            .map(videoMapper::toLightDTO)
            .peek(dto -> dto.setWatched(true))
            .collect(Collectors.toList());

        return PagedResponse.<VideoDTO>builder()
            .content(videoDTOs)
            .page(videoPage.getNumber())
            .size(videoPage.getSize())
            .totalElements(videoPage.getTotalElements())
            .totalPages(videoPage.getTotalPages())
            .last(videoPage.isLast())
            .build();
    }

    /**
     * Create pageable with sorting
     */
    private Pageable createPageable(SearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = Math.min(request.getSize() != null ? request.getSize() : 20, 100);

        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        String direction = request.getSortDirection() != null ? request.getSortDirection() : "desc";

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDirection, mapSortField(sortBy));

        return PageRequest.of(page, size, sort);
    }

    /**
     * Map API sort field to entity field
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "title" -> "title";
            case "year" -> "year";
            case "rating" -> "imdbRating";
            case "created" -> "createdAt";
            default -> "createdAt";
        };
    }

    /**
     * Mark watched videos in DTO list
     */
    private void markWatchedVideos(List<VideoDTO> videoDTOs, UUID userId) {
        Set<UUID> videoIds = videoDTOs.stream()
            .map(VideoDTO::getId)
            .collect(Collectors.toSet());

        // Query all views in batch
        Map<UUID, Boolean> watchedMap = new HashMap<>();
        for (UUID videoId : videoIds) {
            watchedMap.put(videoId, userVideoViewRepository.existsByUserIdAndVideoId(userId, videoId));
        }

        // Update DTOs
        videoDTOs.forEach(dto -> dto.setWatched(watchedMap.getOrDefault(dto.getId(), false)));
    }
}
