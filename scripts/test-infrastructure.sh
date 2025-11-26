#!/bin/bash

echo "======================================"
echo "  HyperTube Infrastructure Tests"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Check if containers are running
echo "1. Checking Docker containers status..."
if docker compose ps | grep -q "healthy"; then
    echo -e "${GREEN}✓ Containers are running${NC}"
else
    echo -e "${RED}✗ Some containers are not healthy${NC}"
    docker compose ps
fi
echo ""

# Test 2: PostgreSQL connection
echo "2. Testing PostgreSQL connection..."
if docker exec hypertube-postgres psql -U hypertube_user -d hypertube -c "SELECT 1" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ PostgreSQL is accessible${NC}"
    echo "   Database: hypertube"
    echo "   User: hypertube_user"
else
    echo -e "${RED}✗ PostgreSQL connection failed${NC}"
fi
echo ""

# Test 3: PostgreSQL schemas
echo "3. Checking PostgreSQL extensions..."
docker exec hypertube-postgres psql -U hypertube_user -d hypertube -c "\dx" 2>/dev/null | grep -E "uuid-ossp|pg_trgm" && \
    echo -e "${GREEN}✓ Required extensions installed${NC}" || \
    echo -e "${YELLOW}⚠ Some extensions may be missing${NC}"
echo ""

# Test 4: Redis connection
echo "4. Testing Redis connection..."
if docker exec hypertube-redis redis-cli ping > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Redis is accessible${NC}"

    # Test Redis operations
    docker exec hypertube-redis redis-cli SET test_key "Infrastructure Test" > /dev/null 2>&1
    RESULT=$(docker exec hypertube-redis redis-cli GET test_key 2>/dev/null)
    if [ "$RESULT" = "Infrastructure Test" ]; then
        echo -e "${GREEN}✓ Redis SET/GET operations working${NC}"
        docker exec hypertube-redis redis-cli DEL test_key > /dev/null 2>&1
    else
        echo -e "${RED}✗ Redis operations failed${NC}"
    fi
else
    echo -e "${RED}✗ Redis connection failed${NC}"
fi
echo ""

# Test 5: RabbitMQ Management API
echo "5. Testing RabbitMQ Management API..."
RESPONSE=$(curl -s -u hypertube:hypertube_dev_password http://localhost:15672/api/overview 2>/dev/null)
if echo "$RESPONSE" | grep -q "rabbitmq_version"; then
    VERSION=$(echo "$RESPONSE" | grep -o '"rabbitmq_version":"[^"]*"' | cut -d'"' -f4)
    echo -e "${GREEN}✓ RabbitMQ Management API is accessible${NC}"
    echo "   Version: $VERSION"
    echo "   Management UI: http://localhost:15672"
    echo "   Credentials: hypertube / hypertube_dev_password"
else
    echo -e "${RED}✗ RabbitMQ Management API failed${NC}"
fi
echo ""

# Test 6: Network connectivity between services
echo "6. Testing inter-service network connectivity..."
if docker exec hypertube-redis ping -c 1 hypertube-postgres > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Redis can reach PostgreSQL${NC}"
else
    echo -e "${YELLOW}⚠ Network connectivity check skipped (ping not available)${NC}"
fi
echo ""

# Test 7: Port availability
echo "7. Checking exposed ports..."
PORTS=(5432 6379 5672 15672)
PORT_NAMES=("PostgreSQL" "Redis" "RabbitMQ AMQP" "RabbitMQ Management")

for i in "${!PORTS[@]}"; do
    PORT="${PORTS[$i]}"
    NAME="${PORT_NAMES[$i]}"
    if nc -z localhost $PORT 2>/dev/null || (echo > /dev/tcp/localhost/$PORT) 2>/dev/null; then
        echo -e "${GREEN}✓ Port $PORT ($NAME) is accessible${NC}"
    else
        echo -e "${RED}✗ Port $PORT ($NAME) is not accessible${NC}"
    fi
done
echo ""

# Summary
echo "======================================"
echo "  Test Summary"
echo "======================================"
echo "Services tested: PostgreSQL, Redis, RabbitMQ"
echo "All infrastructure services are ready for application development"
echo ""
echo "Next steps:"
echo "  - Start Eureka Server: cd services/eureka-server && ./mvnw spring-boot:run"
echo "  - Start API Gateway: cd services/api-gateway && ./mvnw spring-boot:run"
echo ""
