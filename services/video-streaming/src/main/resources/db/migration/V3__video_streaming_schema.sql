-- Video Streaming Service Schema

-- Video torrents (multiple qualities per video)
CREATE TABLE video_torrents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL,
    quality VARCHAR(20) NOT NULL, -- 720p, 1080p, 2160p
    torrent_url VARCHAR(1000),
    magnet_link TEXT,
    size_bytes BIGINT,
    seeds INTEGER DEFAULT 0,
    peers INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_video FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    CONSTRAINT unique_video_quality UNIQUE (video_id, quality)
);

CREATE INDEX idx_video_torrents_video_id ON video_torrents(video_id);
CREATE INDEX idx_video_torrents_quality ON video_torrents(quality);
CREATE INDEX idx_video_torrents_seeds ON video_torrents(seeds DESC);

-- Download jobs for background processing
CREATE TABLE download_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL,
    torrent_id UUID NOT NULL,
    user_id UUID NOT NULL, -- User who requested the download
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending, downloading, converting, completed, failed, cancelled
    progress INTEGER DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    download_speed BIGINT, -- bytes per second
    eta_seconds INTEGER, -- estimated time to completion
    file_path VARCHAR(1000),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_download_video FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    CONSTRAINT fk_download_torrent FOREIGN KEY (torrent_id) REFERENCES video_torrents(id) ON DELETE CASCADE,
    CONSTRAINT fk_download_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_download_jobs_video_id ON download_jobs(video_id);
CREATE INDEX idx_download_jobs_user_id ON download_jobs(user_id);
CREATE INDEX idx_download_jobs_status ON download_jobs(status);
CREATE INDEX idx_download_jobs_created_at ON download_jobs(created_at DESC);

-- Cached videos (completed downloads with 1-month TTL)
CREATE TABLE cached_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL,
    torrent_id UUID NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    format VARCHAR(20) NOT NULL, -- mp4, webm, mkv
    codec VARCHAR(50), -- h264, h265, vp9
    resolution VARCHAR(20), -- 1920x1080, 1280x720
    duration_seconds INTEGER,
    bitrate INTEGER, -- kbps
    cached_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL, -- 1 month from cached_at
    access_count INTEGER DEFAULT 0,
    CONSTRAINT fk_cached_video FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    CONSTRAINT fk_cached_torrent FOREIGN KEY (torrent_id) REFERENCES video_torrents(id) ON DELETE CASCADE,
    CONSTRAINT unique_video_torrent UNIQUE (video_id, torrent_id)
);

CREATE INDEX idx_cached_videos_video_id ON cached_videos(video_id);
CREATE INDEX idx_cached_videos_expires_at ON cached_videos(expires_at);
CREATE INDEX idx_cached_videos_last_accessed ON cached_videos(last_accessed_at);

-- Subtitles
CREATE TABLE subtitles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL,
    language_code VARCHAR(10) NOT NULL, -- en, fr, es, etc.
    language_name VARCHAR(50), -- English, French, Spanish
    file_path VARCHAR(1000) NOT NULL,
    format VARCHAR(10) NOT NULL DEFAULT 'srt', -- srt, vtt, ass
    source VARCHAR(50), -- opensubtitles, subscene, manual
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subtitle_video FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    CONSTRAINT unique_video_language UNIQUE (video_id, language_code)
);

CREATE INDEX idx_subtitles_video_id ON subtitles(video_id);
CREATE INDEX idx_subtitles_language ON subtitles(language_code);

-- Video watch history (tracking what users have watched)
CREATE TABLE video_watch_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    video_id UUID NOT NULL,
    progress_seconds INTEGER DEFAULT 0,
    duration_seconds INTEGER,
    completed BOOLEAN DEFAULT FALSE,
    last_watched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_watch_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_watch_video FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_video_watch UNIQUE (user_id, video_id)
);

CREATE INDEX idx_watch_history_user_id ON video_watch_history(user_id);
CREATE INDEX idx_watch_history_video_id ON video_watch_history(video_id);
CREATE INDEX idx_watch_history_last_watched ON video_watch_history(last_watched_at DESC);

-- Function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for auto-updating updated_at
CREATE TRIGGER update_video_torrents_updated_at
    BEFORE UPDATE ON video_torrents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_download_jobs_updated_at
    BEFORE UPDATE ON download_jobs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
