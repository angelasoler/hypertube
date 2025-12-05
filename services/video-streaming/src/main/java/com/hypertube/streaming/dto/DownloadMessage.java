package com.hypertube.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadMessage implements Serializable {
    private UUID jobId;
    private UUID videoId;
    private UUID torrentId;
    private UUID userId;
    private String magnetLink;
    private String torrentUrl;
    private String quality;
}
