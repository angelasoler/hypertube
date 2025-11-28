package com.hypertube.streaming.controller;

import com.hypertube.streaming.dto.DownloadJobDTO;
import com.hypertube.streaming.entity.DownloadJob;
import com.hypertube.streaming.repository.DownloadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/streaming")
@RequiredArgsConstructor
@Slf4j
public class StreamingController {

    private final DownloadJobRepository downloadJobRepository;

    @GetMapping("/jobs")
    public ResponseEntity<List<DownloadJobDTO>> getAllJobs() {
        List<DownloadJob> jobs = downloadJobRepository.findAll();
        List<DownloadJobDTO> jobDTOs = jobs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobDTOs);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<DownloadJobDTO> getJob(@PathVariable UUID jobId) {
        return downloadJobRepository.findById(jobId)
                .map(this::mapToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs/user/{userId}")
    public ResponseEntity<List<DownloadJobDTO>> getUserJobs(@PathVariable UUID userId) {
        List<DownloadJob> jobs = downloadJobRepository.findByUserId(userId);
        List<DownloadJobDTO> jobDTOs = jobs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobDTOs);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Streaming service is running");
    }

    private DownloadJobDTO mapToDTO(DownloadJob job) {
        return DownloadJobDTO.builder()
                .id(job.getId())
                .videoId(job.getVideoId())
                .torrentId(job.getTorrentId())
                .userId(job.getUserId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .downloadSpeed(job.getDownloadSpeed())
                .etaSeconds(job.getEtaSeconds())
                .filePath(job.getFilePath())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
