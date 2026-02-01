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
public class ConversionMessage implements Serializable {
    private UUID jobId;
    private UUID videoId;
    private String inputFilePath;
    private String outputFilePath;
    private String targetFormat;
    private String targetCodec;
}
