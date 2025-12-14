#!/bin/bash

# Quick script to run E2E tests
# Usage: ./run-tests.sh [test-suite]
# Examples:
#   ./run-tests.sh                 # Run all tests
#   ./run-tests.sh search          # Run only search tests
#   ./run-tests.sh streaming       # Run only streaming tests
#   ./run-tests.sh complete        # Run only complete flow tests

set -e

echo "======================================"
echo "HyperTube E2E Test Runner"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if we're in the tests directory
if [ ! -f "package.json" ]; then
    echo -e "${RED}Error: Must be run from the tests directory${NC}"
    echo "Usage: cd tests && ./run-tests.sh"
    exit 1
fi

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing dependencies...${NC}"
    npm install
    echo ""
fi

# Check if Playwright browsers are installed
if [ ! -d "node_modules/@playwright/test" ]; then
    echo -e "${YELLOW}Installing Playwright...${NC}"
    npm install
fi

# Check if Firefox is installed
echo "Checking Playwright Firefox installation..."
if ! npx playwright install --dry-run firefox 2>&1 | grep -q "is already installed"; then
    echo -e "${YELLOW}Installing Firefox browser...${NC}"
    npx playwright install firefox
    echo ""
fi

# Check if services are running
echo "Checking HyperTube services..."

# Check frontend
if ! curl -s -f http://localhost:3000 > /dev/null 2>&1; then
    echo -e "${RED}Error: Frontend not accessible at http://localhost:3000${NC}"
    echo "Please start services: docker-compose up -d"
    exit 1
fi
echo -e "${GREEN}✓ Frontend is running${NC}"

# Check backend
if ! curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}Error: Backend not accessible at http://localhost:8080${NC}"
    echo "Please start services: docker-compose up -d"
    exit 1
fi
echo -e "${GREEN}✓ Backend is running${NC}"
echo ""

# Check test user
echo "Checking test user..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"e2e_test_user","password":"Test123!@#"}' \
  -w "\n%{http_code}")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" != "200" ]; then
    echo -e "${YELLOW}Test user not found or login failed${NC}"
    echo "Running setup script..."
    ./setup-test-user.sh
    echo ""
else
    echo -e "${GREEN}✓ Test user is ready${NC}"
fi

echo ""
echo "======================================"
echo "Running Tests"
echo "======================================"
echo ""

# Determine which tests to run
TEST_SUITE="${1:-all}"

case "$TEST_SUITE" in
    search)
        echo "Running search tests only..."
        npm run test:search
        ;;
    streaming)
        echo "Running streaming tests only..."
        npm run test:streaming
        ;;
    complete)
        echo "Running complete flow tests only..."
        npm run test:complete
        ;;
    all|*)
        echo "Running all tests..."
        npm test
        ;;
esac

# Check exit code
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}======================================"
    echo "Tests Completed Successfully!"
    echo "======================================${NC}"
    echo ""
    echo "View the HTML report:"
    echo "  npm run report"
else
    echo ""
    echo -e "${RED}======================================"
    echo "Tests Failed!"
    echo "======================================${NC}"
    echo ""
    echo "To debug:"
    echo "  npm run test:debug"
    echo "  npm run test:ui"
    exit 1
fi
