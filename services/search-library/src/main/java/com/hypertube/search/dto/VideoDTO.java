package com.hypertube.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Video response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoDTO {
    private UUID id;
    private String imdbId;
    private String title;
    private Integer year;
    private Integer runtimeMinutes;
    private String synopsis;
    private BigDecimal imdbRating;
    private String posterUrl;
    private String backdropUrl;
    private String language;
    private List<String> genres;
    private List<VideoSourceDTO> sources;
    private List<CastMemberDTO> cast;
    private Boolean watched; // Set when user context is available
}
