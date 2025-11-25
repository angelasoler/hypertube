# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HyperTube is a web-based video streaming platform that uses the BitTorrent protocol to enable just-in-time video streaming. Users can search, stream, and watch videos from multiple external sources with automatic torrent downloading and format conversion handled server-side.

## Core Architecture

### Technology Stack
- **Frontend**: Vue.js with Nuxt framework (SPA architecture)
- **Backend**: Spring Boot Cloud (microservices)
- **Message Queue**: RabbitMQ (for background job processing)
- **Cache**: Redis
- **Database**: PostgreSQL
- **Video Streaming**: BitTorrent protocol with server-side proxy pattern

### Microservices Structure
The application is divided into distinct services:
1. **User Management Service**: Registration, authentication (including OAuth2 with 42 school/Google/GitHub), profile management
2. **Search/Library Service**: Queries external video sources, manages video metadata, handles sorting/filtering
3. **Video Streaming Service**: BitTorrent downloads, format conversion (e.g., MKV to browser-compatible), subtitle management
4. **API Gateway**: RESTful API with OAuth2 authentication

### Key Patterns
- **Worker/Queue Pattern**: Video downloading and conversion use RabbitMQ for non-blocking background processing
- **Proxy Pattern**: Server acts as proxy between browser and BitTorrent network (clients never interact with torrents directly)
- **Content-Based Caching**: Downloaded videos cached for 1 month TTL; Redis used for metadata caching
- **Infinite Scrolling**: Dynamic pagination for video lists without page reloads

## Security Requirements

**CRITICAL**: The following security measures are mandatory and will be extensively checked:
- Never store plain text passwords (use bcrypt or similar)
- Prevent HTML/JavaScript injection in user inputs (sanitize all user-provided data)
- Validate all file uploads strictly (only allow expected video/subtitle formats)
- Prevent SQL injection (use parameterized queries/ORM)
- Implement CSRF protection for all state-changing operations
- Validate and sanitize all API inputs

## Video Streaming Implementation

### Torrent Handling Restrictions
**DO NOT use**: webtorrent, pulsar, or peerflix libraries (prohibited by project requirements). Build torrent streaming from lower-level libraries.

### Streaming Flow
1. User clicks play → API request to streaming service
2. Streaming service adds job to RabbitMQ queue
3. Worker picks up job, initiates BitTorrent download
4. Stream begins when sufficient buffer available (before full download)
5. Format conversion happens in background if needed
6. Video cached on server with 1-month TTL

### Subtitle Management
- English subtitles downloaded automatically when available
- Additional language subtitles based on user's preferred language setting
- Subtitles must be selectable in the integrated web player

## Frontend Requirements

### Browser Compatibility
- Must work with latest Firefox and Chrome versions
- Mobile responsive design required
- Minimum layout: header, main section, footer

### Video Page Features
- Integrated web player (browser-compatible formats)
- Video metadata: name, year, IMDb rating, cover image
- Summary, cast info (producer, director, actors)
- Comment system (users can post and view comments)
- Visual differentiation for watched vs unwatched videos

### Search & Browse
- Search queries minimum 2 external video sources
- Results displayed as thumbnails with key metadata
- Sortable by: name, genre, IMDb rating, production year
- Filterable by genre and other criteria
- Infinite scroll pagination (asynchronous loading)

## Authentication Flow

### Registration Options
1. Standard: username, email, password (with validation)
2. OAuth: 42 school account (mandatory), plus Google/GitHub

### Password Management
- Forgot password → reset email flow
- Profile updates: email, profile picture, preferred language

## API Design

- RESTful endpoints for all functionality
- OAuth2 authentication required
- Stateless design
- All frontend-backend communication via asynchronous AJAX

## Video Library Management

### Homepage
- Display popular videos (sorted by downloads/seeders)
- Pull from external sources
- Thumbnail grid layout

### Video Metadata Tracking
- Mark videos as watched per user
- Store view history
- Track download status and cache availability
