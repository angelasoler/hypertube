package com.hypertube.search.service;

import com.hypertube.search.client.TmdbClient;
import com.hypertube.search.client.YtsClient;
import com.hypertube.search.entity.*;
import com.hypertube.search.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for importing videos from external sources (YTS, TMDB, etc.)
 * and synchronizing them to the local database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoImportService {

    private final VideoRepository videoRepository;
    private final GenreRepository genreRepository;
    private final VideoSourceRepository videoSourceRepository;
    private final CastMemberRepository castMemberRepository;
    private final VideoCastRepository videoCastRepository;
    private final YtsClient ytsClient;
    private final TmdbClient tmdbClient;

    /**
     * Import popular movies from YTS to populate the database
     *
     * @param numPages Number of pages to import (each page has ~20 movies)
     * @return Number of movies imported
     */
    @Transactional
    public int importPopularMoviesFromYts(int numPages) {
        log.info("Starting import of {} pages of popular movies from YTS", numPages);
        int imported = 0;

        for (int page = 1; page <= numPages; page++) {
            try {
                Map<String, Object> response = ytsClient.getPopularMovies(page).block();

                if (response == null || !response.containsKey("data")) {
                    log.warn("No data received from YTS for page {}", page);
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");

                if (!data.containsKey("movies")) {
                    log.warn("No movies found in YTS response for page {}", page);
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> movies = (List<Map<String, Object>>) data.get("movies");

                for (Map<String, Object> movieData : movies) {
                    try {
                        importYtsMovie(movieData);
                        imported++;
                    } catch (Exception e) {
                        log.error("Failed to import movie: {}", movieData.get("title"), e);
                    }
                }

                log.info("Imported page {}/{}: {} movies total", page, numPages, imported);

                // Rate limiting - avoid overwhelming the API
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("Error importing page {} from YTS", page, e);
            }
        }

        log.info("Completed import: {} movies imported from YTS", imported);
        return imported;
    }

    /**
     * Import a single movie from YTS data
     */
    @Transactional
    public Video importYtsMovie(Map<String, Object> movieData) {
        String imdbId = (String) movieData.get("imdb_code");
        String title = (String) movieData.get("title_long");

        if (imdbId == null || imdbId.isEmpty()) {
            log.warn("Skipping movie without IMDb ID: {}", title);
            return null;
        }

        // Check if already exists
        Optional<Video> existing = videoRepository.findByImdbId(imdbId);
        if (existing.isPresent()) {
            log.debug("Movie already exists: {}", title);
            return existing.get();
        }

        // Create video entity
        Video video = new Video();
        video.setImdbId(imdbId);
        video.setTitle((String) movieData.get("title"));

        // Parse year
        Object yearObj = movieData.get("year");
        if (yearObj instanceof Integer) {
            video.setYear((Integer) yearObj);
        } else if (yearObj instanceof String) {
            try {
                video.setYear(Integer.parseInt((String) yearObj));
            } catch (NumberFormatException e) {
                log.warn("Invalid year format for {}: {}", title, yearObj);
            }
        }

        // Parse runtime
        Object runtimeObj = movieData.get("runtime");
        if (runtimeObj instanceof Integer) {
            video.setRuntimeMinutes((Integer) runtimeObj);
        }

        // Synopsis
        video.setSynopsis((String) movieData.get("synopsis"));

        // IMDb rating
        Object ratingObj = movieData.get("rating");
        if (ratingObj instanceof Number) {
            video.setImdbRating(BigDecimal.valueOf(((Number) ratingObj).doubleValue()));
        }

        // Images
        video.setPosterUrl((String) movieData.get("medium_cover_image"));
        video.setBackdropUrl((String) movieData.get("background_image_original"));

        video.setLanguage((String) movieData.get("language"));

        // Save video
        video = videoRepository.save(video);
        log.info("Imported video: {} ({})", video.getTitle(), video.getImdbId());

        // Import genres
        @SuppressWarnings("unchecked")
        List<String> genreNames = (List<String>) movieData.get("genres");
        if (genreNames != null && !genreNames.isEmpty()) {
            importGenres(video, genreNames);
        }

        // Import torrent sources
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> torrents = (List<Map<String, Object>>) movieData.get("torrents");
        if (torrents != null && !torrents.isEmpty()) {
            importTorrentSources(video, torrents);
        }

        // Optionally fetch additional metadata from TMDB
        enrichWithTmdbData(video);

        return video;
    }

    /**
     * Import genres for a video
     */
    private void importGenres(Video video, List<String> genreNames) {
        for (String genreName : genreNames) {
            Genre genre = genreRepository.findByName(genreName)
                .orElseGet(() -> {
                    Genre newGenre = new Genre();
                    newGenre.setName(genreName);
                    return genreRepository.save(newGenre);
                });

            // Create video-genre association using builder
            VideoGenre videoGenre = VideoGenre.builder()
                .video(video)
                .genre(genre)
                .build();

            if (video.getVideoGenres() == null) {
                video.setVideoGenres(new HashSet<>());
            }
            video.getVideoGenres().add(videoGenre);
        }

        videoRepository.save(video);
    }

    /**
     * Import torrent sources for a video
     */
    private void importTorrentSources(Video video, List<Map<String, Object>> torrents) {
        for (Map<String, Object> torrentData : torrents) {
            String quality = (String) torrentData.get("quality");
            String hash = (String) torrentData.get("hash");
            String url = (String) torrentData.get("url");

            if (hash == null || hash.isEmpty()) {
                continue;
            }

            // Check if source already exists
            Optional<VideoSource> existing = videoSourceRepository
                .findByVideoAndQualityAndSourceType(video, quality, "yts");

            if (existing.isPresent()) {
                continue;
            }

            // Build magnet link
            String magnetLink = buildMagnetLink(hash, video.getTitle());

            VideoSource source = VideoSource.builder()
                .video(video)
                .sourceType("yts")
                .quality(quality)
                .torrentHash(hash)
                .magnetUrl(magnetLink)
                .seeds(((Number) torrentData.getOrDefault("seeds", 0)).intValue())
                .peers(((Number) torrentData.getOrDefault("peers", 0)).intValue())
                .sizeBytes(((Number) torrentData.getOrDefault("size_bytes", 0)).longValue())
                .build();

            videoSourceRepository.save(source);
        }

        log.debug("Imported {} torrent sources for: {}", torrents.size(), video.getTitle());
    }

    /**
     * Build a magnet link from torrent hash
     */
    private String buildMagnetLink(String hash, String displayName) {
        String encodedName = displayName != null ? displayName.replace(" ", "+") : "video";
        return String.format(
            "magnet:?xt=urn:btih:%s&dn=%s&tr=udp://tracker.opentrackr.org:1337/announce&tr=udp://tracker.leechers-paradise.org:6969/announce",
            hash,
            encodedName
        );
    }

    /**
     * Enrich video with TMDB metadata (cast, crew, additional images)
     */
    @Async
    public void enrichWithTmdbData(Video video) {
        try {
            Map<String, Object> tmdbData = tmdbClient.getMovieByImdbId(video.getImdbId()).block();

            if (tmdbData == null) {
                log.debug("No TMDB data found for: {}", video.getImdbId());
                return;
            }

            // Extract movie results from TMDB find endpoint
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> movieResults = (List<Map<String, Object>>) tmdbData.get("movie_results");

            if (movieResults == null || movieResults.isEmpty()) {
                log.debug("No movie results from TMDB for: {}", video.getImdbId());
                return;
            }

            Map<String, Object> movieData = movieResults.get(0);
            Long tmdbId = ((Number) movieData.get("id")).longValue();

            // Get detailed movie info and credits
            Map<String, Object> details = tmdbClient.getMovieDetails(tmdbId).block();
            Map<String, Object> credits = tmdbClient.getMovieCredits(tmdbId).block();

            if (details != null) {
                enrichVideoWithDetails(video, details);
            }

            if (credits != null) {
                enrichVideoWithCredits(video, credits);
            }

            videoRepository.save(video);
            log.info("Enriched video with TMDB data: {}", video.getTitle());

        } catch (Exception e) {
            log.error("Failed to enrich video {} with TMDB data", video.getImdbId(), e);
        }
    }

    /**
     * Enrich video with TMDB details
     */
    private void enrichVideoWithDetails(Video video, Map<String, Object> details) {
        // Update poster if better quality available
        String posterPath = (String) details.get("poster_path");
        if (posterPath != null && !posterPath.isEmpty()) {
            video.setPosterUrl("https://image.tmdb.org/t/p/w500" + posterPath);
        }

        String backdropPath = (String) details.get("backdrop_path");
        if (backdropPath != null && !backdropPath.isEmpty()) {
            video.setBackdropUrl("https://image.tmdb.org/t/p/original" + backdropPath);
        }

        // Update synopsis if more detailed
        String overview = (String) details.get("overview");
        if (overview != null && !overview.isEmpty() && overview.length() > (video.getSynopsis() != null ? video.getSynopsis().length() : 0)) {
            video.setSynopsis(overview);
        }
    }

    /**
     * Enrich video with TMDB credits (cast and crew)
     */
    private void enrichVideoWithCredits(Video video, Map<String, Object> credits) {
        // Import cast members
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cast = (List<Map<String, Object>>) credits.get("cast");
        if (cast != null) {
            importCastMembers(video, cast, "actor");
        }

        // Import crew members (director, producers)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> crew = (List<Map<String, Object>>) credits.get("crew");
        if (crew != null) {
            importCrewMembers(video, crew);
        }
    }

    /**
     * Import cast members for a video
     */
    private void importCastMembers(Video video, List<Map<String, Object>> cast, String role) {
        int order = 0;
        // Limit to top 10 cast members
        for (Map<String, Object> personData : cast.stream().limit(10).collect(Collectors.toList())) {
            String name = (String) personData.get("name");
            String character = (String) personData.get("character");
            String profilePath = (String) personData.get("profile_path");

            if (name == null || name.isEmpty()) {
                continue;
            }

            CastMember castMember = castMemberRepository.findByName(name)
                .orElseGet(() -> {
                    CastMember newMember = new CastMember();
                    newMember.setName(name);
                    if (profilePath != null && !profilePath.isEmpty()) {
                        newMember.setProfilePhotoUrl("https://image.tmdb.org/t/p/w185" + profilePath);
                    }
                    return castMemberRepository.save(newMember);
                });

            // Create video-cast association
            VideoCast videoCast = VideoCast.builder()
                .video(video)
                .castMember(castMember)
                .role(role)
                .characterName(character)
                .displayOrder(order++)
                .build();

            videoCastRepository.save(videoCast);
        }
    }

    /**
     * Import crew members (director, producers, writers)
     */
    private void importCrewMembers(Video video, List<Map<String, Object>> crew) {
        for (Map<String, Object> personData : crew) {
            String name = (String) personData.get("name");
            String job = (String) personData.get("job");
            String profilePath = (String) personData.get("profile_path");

            if (name == null || name.isEmpty() || job == null) {
                continue;
            }

            // Only import specific roles
            String role = switch (job.toLowerCase()) {
                case "director" -> "director";
                case "producer" -> "producer";
                case "screenplay", "writer" -> "writer";
                default -> null;
            };

            if (role == null) {
                continue;
            }

            CastMember castMember = castMemberRepository.findByName(name)
                .orElseGet(() -> {
                    CastMember newMember = new CastMember();
                    newMember.setName(name);
                    if (profilePath != null && !profilePath.isEmpty()) {
                        newMember.setProfilePhotoUrl("https://image.tmdb.org/t/p/w185" + profilePath);
                    }
                    return castMemberRepository.save(newMember);
                });

            // Check if already exists
            if (videoCastRepository.existsByVideoAndCastMemberAndRole(video, castMember, role)) {
                continue;
            }

            VideoCast videoCast = VideoCast.builder()
                .video(video)
                .castMember(castMember)
                .role(role)
                .displayOrder(0)
                .build();

            videoCastRepository.save(videoCast);
        }
    }

    /**
     * Search and import movie by query
     *
     * @param query Search query
     * @return List of imported videos
     */
    @Transactional
    public List<Video> searchAndImport(String query) {
        log.info("Searching and importing videos for query: {}", query);
        List<Video> imported = new ArrayList<>();

        try {
            // Search YTS
            Map<String, Object> ytsResponse = ytsClient.searchMovies(query, 1).block();

            if (ytsResponse != null && ytsResponse.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) ytsResponse.get("data");

                if (data.containsKey("movies")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> movies = (List<Map<String, Object>>) data.get("movies");

                    for (Map<String, Object> movieData : movies) {
                        try {
                            Video video = importYtsMovie(movieData);
                            if (video != null) {
                                imported.add(video);
                            }
                        } catch (Exception e) {
                            log.error("Failed to import movie from search: {}", movieData.get("title"), e);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error searching and importing for query: {}", query, e);
        }

        log.info("Imported {} videos for query: {}", imported.size(), query);
        return imported;
    }
}
