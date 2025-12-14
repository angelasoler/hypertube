#!/bin/bash

# Setup script for E2E test user
# This script creates a test user for running E2E tests

set -e

echo "======================================"
echo "HyperTube E2E Test User Setup"
echo "======================================"
echo ""

# Configuration
API_URL="${API_URL:-http://localhost:8080}"
USERNAME="e2e_test_user"
EMAIL="e2e@test.com"
PASSWORD="Test123!@#"
FIRST_NAME="E2E"
LAST_NAME="Test User"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if curl is installed
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl is not installed${NC}"
    exit 1
fi

# Check if jq is installed (optional but helpful)
if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}Warning: jq is not installed (optional)${NC}"
    HAS_JQ=false
else
    HAS_JQ=true
fi

echo "Checking if API is accessible..."
if ! curl -s -f "${API_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}Error: Cannot connect to API at ${API_URL}${NC}"
    echo "Please ensure HyperTube services are running:"
    echo "  docker-compose up -d"
    exit 1
fi

echo -e "${GREEN}✓ API is accessible${NC}"
echo ""

# Check if user already exists
echo "Checking if test user already exists..."
LOGIN_RESPONSE=$(curl -s -X POST "${API_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}" \
  -w "\n%{http_code}")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${YELLOW}Test user already exists and login is successful${NC}"
    echo "Username: ${USERNAME}"
    echo "Password: ${PASSWORD}"
    echo ""
    echo "You can proceed with running tests:"
    echo "  cd tests"
    echo "  npm test"
    exit 0
fi

echo "Test user does not exist. Creating..."
echo ""

# Create test user
echo "Registering test user..."
REGISTER_RESPONSE=$(curl -s -X POST "${API_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\":\"${USERNAME}\",
    \"email\":\"${EMAIL}\",
    \"password\":\"${PASSWORD}\",
    \"firstName\":\"${FIRST_NAME}\",
    \"lastName\":\"${LAST_NAME}\"
  }" \
  -w "\n%{http_code}")

HTTP_CODE=$(echo "$REGISTER_RESPONSE" | tail -n 1)
RESPONSE_BODY=$(echo "$REGISTER_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo -e "${GREEN}✓ Test user created successfully${NC}"
    echo ""
    echo "Test User Credentials:"
    echo "====================="
    echo "Username: ${USERNAME}"
    echo "Email:    ${EMAIL}"
    echo "Password: ${PASSWORD}"
    echo ""

    # Verify login works
    echo "Verifying login..."
    LOGIN_RESPONSE=$(curl -s -X POST "${API_URL}/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}" \
      -w "\n%{http_code}")

    LOGIN_HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n 1)

    if [ "$LOGIN_HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ Login verification successful${NC}"

        if [ "$HAS_JQ" = true ]; then
            LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')
            TOKEN=$(echo "$LOGIN_BODY" | jq -r '.token // .accessToken // empty')
            if [ -n "$TOKEN" ]; then
                echo "Token received: ${TOKEN:0:20}..."
            fi
        fi
    else
        echo -e "${YELLOW}Warning: Login verification returned HTTP ${LOGIN_HTTP_CODE}${NC}"
    fi

    echo ""
    echo "Setup complete! You can now run the E2E tests:"
    echo "  cd tests"
    echo "  npm install"
    echo "  npx playwright install firefox"
    echo "  npm test"

elif [ "$HTTP_CODE" = "409" ] || [ "$HTTP_CODE" = "400" ]; then
    echo -e "${YELLOW}User might already exist (HTTP ${HTTP_CODE})${NC}"
    echo ""
    echo "Trying to login with existing credentials..."

    LOGIN_RESPONSE=$(curl -s -X POST "${API_URL}/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}" \
      -w "\n%{http_code}")

    LOGIN_HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n 1)

    if [ "$LOGIN_HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ Login successful with existing user${NC}"
        echo ""
        echo "Username: ${USERNAME}"
        echo "Password: ${PASSWORD}"
    else
        echo -e "${RED}Error: Cannot login with test user credentials${NC}"
        echo "HTTP Code: ${LOGIN_HTTP_CODE}"
        echo ""
        echo "You may need to:"
        echo "1. Manually delete the existing user from database"
        echo "2. Or use different credentials in test-data.json"
        exit 1
    fi

else
    echo -e "${RED}Error: Failed to create test user${NC}"
    echo "HTTP Code: ${HTTP_CODE}"
    echo "Response: ${RESPONSE_BODY}"
    exit 1
fi

echo ""
echo "======================================"
echo "Setup Complete!"
echo "======================================"
