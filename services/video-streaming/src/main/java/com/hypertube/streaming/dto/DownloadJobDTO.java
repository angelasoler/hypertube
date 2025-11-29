package com.hypertube.streaming.dto;

import com.hypertube.streaming.entity.DownloadJob.DownloadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadJobDTO {
    private UUID id;
    private UUID videoId;
    private UUID torrentId;
    private UUID userId;
    private DownloadStatus status;
    private Integer progress;
    private Long downloadSpeed;
    private Integer etaSeconds;
    private String filePath;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
