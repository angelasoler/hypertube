# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

# First, build and install the torrent-lib dependency
COPY torrent-lib/pom.xml ./torrent-lib/
COPY torrent-lib/src ./torrent-lib/src

WORKDIR /workspace/torrent-lib
RUN mvn clean install -DskipTests

# Now build the video-streaming service
WORKDIR /workspace/video-streaming

# Copy pom.xml and source code
COPY video-streaming/pom.xml .
COPY video-streaming/src ./src

# Build the service (dependencies will be downloaded during build)
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install required packages for video processing
RUN apk add --no-cache \
    curl \
    ffmpeg \
    ffmpeg-libs

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create necessary directories
RUN mkdir -p /tmp/hypertube/videos /tmp/hypertube/temp /tmp/hypertube/subtitles /tmp/hypertube/downloads && \
    chown -R spring:spring /tmp/hypertube

USER spring:spring

# Copy jar from build stage
COPY --from=build /workspace/video-streaming/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
