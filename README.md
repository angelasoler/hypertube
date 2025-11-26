# HyperTube

A web-based video streaming platform that uses the BitTorrent protocol to enable just-in-time video streaming.

## Features

- Search and stream videos from multiple external sources
- Just-in-time BitTorrent streaming with server-side proxy
- Automatic format conversion for browser compatibility
- Multi-language subtitle support
- User authentication (standard + OAuth2 with 42 school/Google/GitHub)
- Comment system and watch history
- Mobile-responsive design

## Technology Stack

### Backend
- Spring Boot Cloud (microservices architecture)
- PostgreSQL (database)
- Redis (caching)
- RabbitMQ (message queue)
- libtorrent4j (BitTorrent integration)
- FFmpeg (video conversion)

### Frontend
- Vue.js with Nuxt framework
- Vuex (state management)
- Tailwind CSS (styling)

## Architecture

The application follows a microservices architecture:

1. **Eureka Server** - Service discovery
2. **API Gateway** - Request routing and OAuth2 authentication
3. **User Management Service** - Authentication and profile management
4. **Search/Library Service** - Video search and metadata management
5. **Video Streaming Service** - BitTorrent downloads and streaming
6. **Streaming Worker** - Background video processing and conversion
7. **Comment Service** - User comments and ratings

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 17+
- Node.js 18+
- Maven 3.8+

### Development Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd hypertube
```

2. Start infrastructure services:
```bash
docker-compose up -d postgres redis rabbitmq
```

3. Start backend services:
```bash
cd services
./mvnw spring-boot:run
```

4. Start frontend:
```bash
cd frontend
npm install
npm run dev
```

5. Access the application:
- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- Eureka Dashboard: http://localhost:8761

## Project Structure

```
hypertube/
├── services/                    # Backend microservices
│   ├── eureka-server/          # Service discovery
│   ├── api-gateway/            # API Gateway
│   ├── user-management/        # User service
│   ├── search-library/         # Search service
│   ├── video-streaming/        # Streaming service
│   ├── streaming-worker/       # Background worker
│   └── comment-service/        # Comment service
├── frontend/                    # Nuxt.js frontend
├── doc/                        # Documentation
├── docker-compose.yml          # Docker orchestration
└── CLAUDE.md                   # Development guide
```

## Security

This application implements comprehensive security measures:

- Bcrypt password hashing
- JWT authentication with token refresh
- Input validation and sanitization
- SQL injection prevention
- XSS protection
- CSRF protection
- Rate limiting

## License

[Add your license here]

## Contributors

[Add contributors here]
