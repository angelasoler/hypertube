-- Initial database setup script
-- This script runs automatically when PostgreSQL container is first created

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create databases for each service if needed
-- Main database is created via POSTGRES_DB environment variable

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE hypertube TO hypertube_user;

-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS videos;
CREATE SCHEMA IF NOT EXISTS comments;
CREATE SCHEMA IF NOT EXISTS streaming;

-- Set search path
ALTER DATABASE hypertube SET search_path TO public, users, videos, comments, streaming;

-- Initial comment
COMMENT ON DATABASE hypertube IS 'HyperTube video streaming platform database';
