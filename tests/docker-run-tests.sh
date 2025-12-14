#!/bin/bash

# Script to run E2E tests in Docker container
# This avoids the need to install Node.js and Playwright on the host system

set -e

echo "======================================"
echo "HyperTube E2E Tests (Docker Mode)"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

# Check if we're in the tests directory
if [ ! -f "package.json" ]; then
    echo -e "${RED}Error: Must be run from the tests directory${NC}"
    echo "Usage: cd tests && ./docker-run-tests.sh"
    exit 1
fi

# Check if services are running
echo "Checking if HyperTube services are running..."

if ! curl -s -f http://localhost:3000 > /dev/null 2>&1; then
    echo -e "${RED}Error: Frontend not accessible at http://localhost:3000${NC}"
    echo "Please start services: docker compose up -d"
    exit 1
fi
echo -e "${GREEN}✓ Frontend is running${NC}"

if ! curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}Error: Backend not accessible at http://localhost:8080${NC}"
    echo "Please start services: docker compose up -d"
    exit 1
fi
echo -e "${GREEN}✓ Backend is running${NC}"

echo ""
echo "Building test container..."
docker build -t hypertube-e2e-tests -f Dockerfile.test .

echo ""
echo "======================================"
echo "Running Tests"
echo "======================================"
echo ""

# Run tests in Docker with network access to host
docker run --rm \
  --name hypertube-tests \
  --network host \
  -v "$(pwd)/test-results:/app/test-results" \
  -v "$(pwd)/playwright-report:/app/playwright-report" \
  hypertube-e2e-tests

# Check exit code
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}======================================"
    echo "Tests Completed Successfully!"
    echo "======================================${NC}"
    echo ""
    echo "View the HTML report:"
    echo "  Open playwright-report/index.html in your browser"
else
    echo ""
    echo -e "${RED}======================================"
    echo "Tests Failed!"
    echo "======================================${NC}"
    echo ""
    echo "Check the test results in:"
    echo "  - test-results/ directory"
    echo "  - playwright-report/ directory"
    exit 1
fi
