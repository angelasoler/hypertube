#!/bin/bash

# Security Configuration Validation Script
# Checks that HyperTube is configured with proper security settings

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

echo "========================================="
echo "HyperTube Security Configuration Check"
echo "========================================="
echo ""

# Function to print error
error() {
    echo -e "${RED}[ERROR]${NC} $1"
    ((ERRORS++))
}

# Function to print warning
warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
    ((WARNINGS++))
}

# Function to print success
success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

# Check if .env file exists
echo "1. Checking environment configuration..."
if [ ! -f .env ]; then
    error ".env file not found. Copy .env.example to .env and configure it."
else
    success ".env file exists"

    # Source the .env file
    source .env

    # Check JWT_SECRET
    if [ -z "$JWT_SECRET" ]; then
        error "JWT_SECRET is not set in .env"
    elif [ ${#JWT_SECRET} -lt 32 ]; then
        error "JWT_SECRET is too short (${#JWT_SECRET} characters). Minimum 32 required, 64+ recommended."
    elif [ ${#JWT_SECRET} -lt 64 ]; then
        warn "JWT_SECRET is acceptable but short (${#JWT_SECRET} characters). 64+ recommended."
    else
        success "JWT_SECRET is properly configured (${#JWT_SECRET} characters)"
    fi

    # Check for forbidden JWT secrets
    if echo "$JWT_SECRET" | grep -qi "change"; then
        error "JWT_SECRET contains 'change' - appears to be a default/placeholder value"
    fi
    if echo "$JWT_SECRET" | grep -qi "secret"; then
        error "JWT_SECRET contains 'secret' - appears to be a weak/default value"
    fi
    if echo "$JWT_SECRET" | grep -qi "password"; then
        error "JWT_SECRET contains 'password' - appears to be a weak/default value"
    fi

    # Check DB_PASSWORD
    if [ -z "$DB_PASSWORD" ]; then
        error "DB_PASSWORD is not set in .env"
    elif [ ${#DB_PASSWORD} -lt 16 ]; then
        error "DB_PASSWORD is too short (${#DB_PASSWORD} characters). Minimum 16 required, 32+ recommended."
    elif echo "$DB_PASSWORD" | grep -qi "password"; then
        error "DB_PASSWORD contains 'password' - appears to be a weak/default value"
    elif echo "$DB_PASSWORD" | grep -qi "change"; then
        error "DB_PASSWORD contains 'change' - appears to be a placeholder value"
    else
        success "DB_PASSWORD is properly configured"
    fi

    # Check REDIS_PASSWORD
    if [ -z "$REDIS_PASSWORD" ]; then
        error "REDIS_PASSWORD is not set in .env"
    elif [ ${#REDIS_PASSWORD} -lt 16 ]; then
        error "REDIS_PASSWORD is too short (${#REDIS_PASSWORD} characters). Minimum 16 required, 32+ recommended."
    else
        success "REDIS_PASSWORD is properly configured"
    fi

    # Check RABBITMQ_PASSWORD
    if [ -z "$RABBITMQ_PASSWORD" ]; then
        error "RABBITMQ_PASSWORD is not set in .env"
    elif [ ${#RABBITMQ_PASSWORD} -lt 16 ]; then
        error "RABBITMQ_PASSWORD is too short (${#RABBITMQ_PASSWORD} characters). Minimum 16 required, 32+ recommended."
    else
        success "RABBITMQ_PASSWORD is properly configured"
    fi
fi

echo ""
echo "2. Checking git configuration..."
if git check-ignore -q .env; then
    success ".env is in .gitignore (secrets won't be committed)"
else
    error ".env is NOT in .gitignore - secrets may be committed to git!"
fi

# Check if .env was ever committed
if git log --all --full-history -- .env 2>/dev/null | grep -q "commit"; then
    error ".env file has been committed to git history! Secrets may be exposed!"
    echo "       Run: git filter-repo --path .env --invert-paths (requires git-filter-repo)"
else
    success ".env has never been committed to git"
fi

echo ""
echo "3. Checking Docker Compose configuration..."
# Check for hardcoded passwords
if grep -E "PASSWORD.*:.*[^}]$" docker-compose.yml | grep -v "REDIS_PASSWORD\}" | grep -v "DB_PASSWORD\}" | grep -v "RABBITMQ_PASSWORD\}" | grep -v "JWT_SECRET\}" | grep -q "."; then
    error "Found hardcoded passwords in docker-compose.yml"
    grep -E "PASSWORD.*:.*[^}]$" docker-compose.yml | grep -v "REDIS_PASSWORD\}" | grep -v "DB_PASSWORD\}" | grep -v "RABBITMQ_PASSWORD\}" | grep -v "JWT_SECRET\}"
else
    success "No hardcoded passwords in docker-compose.yml"
fi

echo ""
echo "4. Checking profile configuration..."
# Check if SPRING_PROFILES_ACTIVE is set to dev
if grep -q "SPRING_PROFILES_ACTIVE.*dev" docker-compose.yml 2>/dev/null; then
    warn "Found 'dev' profile in docker-compose.yml - should be 'docker' for production"
else
    success "No 'dev' profile found in main docker-compose.yml"
fi

echo ""
echo "5. Checking application configuration files..."
# Check for weak defaults in application.yml
if grep -q "JWT_SECRET:.*your-" services/api-gateway/src/main/resources/application.yml 2>/dev/null; then
    error "Weak JWT default found in application.yml"
else
    success "No weak JWT defaults in application.yml"
fi

# Check that passwords don't have defaults
if grep -E "PASSWORD:-[^}]" services/api-gateway/src/main/resources/application.yml 2>/dev/null | grep -v "PASSWORD:-}" | grep -q "."; then
    error "Found password with non-empty default in application.yml"
else
    success "No password defaults in application.yml"
fi

echo ""
echo "6. Checking rate limiting configuration..."
if grep -q "RequestRateLimiter" services/api-gateway/src/main/resources/application.yml 2>/dev/null; then
    success "Rate limiting is configured"

    # Check for authentication endpoint rate limits
    if grep -A5 "user-auth-login" services/api-gateway/src/main/resources/application.yml | grep -q "replenishRate"; then
        success "Authentication endpoints have dedicated rate limits"
    else
        warn "Authentication endpoints may not have dedicated rate limits"
    fi
else
    error "Rate limiting is not configured"
fi

echo ""
echo "7. Checking Redis security..."
if grep -q "requirepass" docker-compose.yml 2>/dev/null; then
    success "Redis password protection is configured"
else
    warn "Redis may not have password protection configured"
fi

echo ""
echo "========================================="
echo "Security Check Summary"
echo "========================================="
echo -e "Errors:   ${RED}$ERRORS${NC}"
echo -e "Warnings: ${YELLOW}$WARNINGS${NC}"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}All security checks passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Review SECURITY.md for deployment checklist"
    echo "  2. Review SETUP_SECURITY.md for setup instructions"
    echo "  3. Start services: docker-compose up -d"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}Security checks passed with warnings.${NC}"
    echo "Review warnings above and fix if necessary."
    echo ""
    echo "For production deployment, all warnings should be resolved."
    exit 0
else
    echo -e "${RED}Security checks failed!${NC}"
    echo ""
    echo "Please fix the errors above before starting the application."
    echo "See SETUP_SECURITY.md for detailed instructions."
    exit 1
fi
