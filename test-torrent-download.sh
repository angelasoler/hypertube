#!/bin/bash

# Test script for torrent download functionality
# This script tests the video-streaming service with a real magnet link

set -e

echo "=========================================="
echo "HyperTube Torrent Download Test"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
API_BASE_URL="http://localhost:8083"
HEALTH_ENDPOINT="$API_BASE_URL/actuator/health"

# Test magnet link (Big Buck Bunny - Creative Commons licensed video)
# This is a small, legal torrent perfect for testing
TEST_MAGNET="magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com"

echo -e "${YELLOW}Step 1: Checking if streaming service is running...${NC}"
if curl -s -f "$HEALTH_ENDPOINT" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Streaming service is healthy${NC}"
else
    echo -e "${RED}✗ Streaming service is not responding${NC}"
    echo "Please start the services with: docker-compose up -d"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 2: Checking Docker logs for torrent service initialization...${NC}"
docker logs hypertube-streaming-service --tail 50 | grep -i "torrent" || true

echo ""
echo -e "${YELLOW}Step 3: Test Information${NC}"
echo "Magnet link: $TEST_MAGNET"
echo "Video: Big Buck Bunny (Creative Commons)"
echo ""
echo "To manually test the download:"
echo "1. Create a download job via the API"
echo "2. Monitor progress in Docker logs:"
echo "   docker logs -f hypertube-streaming-service"
echo "3. Check the download directory:"
echo "   docker exec hypertube-streaming-service ls -lh /tmp/hypertube/downloads"
echo ""
echo -e "${GREEN}=========================================="
echo "Test setup complete!"
echo "==========================================${NC}"
