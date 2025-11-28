package com.hypertube.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for VideoSource response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSourceDTO {
    private UUID id;
    private String sourceType;
    private String quality;
    private String torrentHash;
    private String magnetUrl;
    private Integer seeds;
    private Integer peers;
    private Long sizeBytes;
}
