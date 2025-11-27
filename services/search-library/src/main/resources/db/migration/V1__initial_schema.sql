-- Search/Library Service Database Schema
-- Manages video metadata, torrent sources, cast information, and user views

-- Videos table: Core metadata for movies/TV shows
CREATE TABLE videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    imdb_id VARCHAR(20) UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    year INTEGER,
    runtime_minutes INTEGER,
    synopsis TEXT,
    imdb_rating DECIMAL(3, 1),
    poster_url VARCHAR(1000),
    backdrop_url VARCHAR(1000),
    language VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for video searches
CREATE INDEX idx_videos_imdb_id ON videos(imdb_id);
CREATE INDEX idx_videos_title ON videos(title);
CREATE INDEX idx_videos_year ON videos(year);
CREATE INDEX idx_videos_imdb_rating ON videos(imdb_rating);
CREATE INDEX idx_videos_created_at ON videos(created_at DESC);

-- Video genres table: Many-to-many relationship
CREATE TABLE genres (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE video_genres (
    video_id UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    genre_id UUID NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (video_id, genre_id)
);

CREATE INDEX idx_video_genres_genre_id ON video_genres(genre_id);

-- Video sources table: Torrent sources for each video
CREATE TABLE video_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    source_type VARCHAR(50) NOT NULL, -- 'yts', 'eztv', 'custom'
    quality VARCHAR(20) NOT NULL, -- '720p', '1080p', '2160p', etc.
    torrent_hash VARCHAR(40) NOT NULL,
    magnet_url TEXT NOT NULL,
    seeds INTEGER DEFAULT 0,
    peers INTEGER DEFAULT 0,
    size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(video_id, source_type, quality)
);

CREATE INDEX idx_video_sources_video_id ON video_sources(video_id);
CREATE INDEX idx_video_sources_torrent_hash ON video_sources(torrent_hash);
CREATE INDEX idx_video_sources_quality ON video_sources(quality);
CREATE INDEX idx_video_sources_seeds ON video_sources(seeds DESC);

-- Cast members table: Actors, directors, producers
CREATE TABLE cast_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    profile_photo_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name)
);

CREATE INDEX idx_cast_members_name ON cast_members(name);

-- Video cast junction table
CREATE TABLE video_cast (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    cast_member_id UUID NOT NULL REFERENCES cast_members(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL, -- 'actor', 'director', 'producer', 'writer'
    character_name VARCHAR(255), -- Only for actors
    display_order INTEGER DEFAULT 0, -- For sorting
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(video_id, cast_member_id, role, character_name)
);

CREATE INDEX idx_video_cast_video_id ON video_cast(video_id);
CREATE INDEX idx_video_cast_cast_member_id ON video_cast(cast_member_id);
CREATE INDEX idx_video_cast_role ON video_cast(role);

-- User video views: Track which videos users have watched
-- Note: user_id references User Management Service (not FK due to microservices)
CREATE TABLE user_video_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL, -- References user from User Management Service
    video_id UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    last_watched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, video_id)
);

CREATE INDEX idx_user_video_views_user_id ON user_video_views(user_id);
CREATE INDEX idx_user_video_views_video_id ON user_video_views(video_id);
CREATE INDEX idx_user_video_views_last_watched ON user_video_views(last_watched_at DESC);

-- Seed common genres
INSERT INTO genres (name) VALUES
    ('Action'),
    ('Adventure'),
    ('Animation'),
    ('Comedy'),
    ('Crime'),
    ('Drama'),
    ('Fantasy'),
    ('Horror'),
    ('Mystery'),
    ('Romance'),
    ('Science Fiction'),
    ('Thriller'),
    ('Documentary'),
    ('Western');
