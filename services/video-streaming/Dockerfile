# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install required packages for video processing and torrents
RUN apk add --no-cache \
    curl \
    ffmpeg \
    ffmpeg-libs \
    boost-system \
    boost-python3 \
    libtorrent \
    libtorrent-dev

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create necessary directories
RUN mkdir -p /tmp/hypertube/videos /tmp/hypertube/temp /tmp/hypertube/subtitles /tmp/hypertube/downloads && \
    chown -R spring:spring /tmp/hypertube

USER spring:spring

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
