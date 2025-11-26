# Security Setup Guide

This guide walks through setting up HyperTube with proper security configuration.

## Prerequisites

- Docker and Docker Compose
- OpenSSL (for generating secure passwords)
- Git

## Step 1: Clone and Navigate

```bash
git clone <repository-url>
cd hypertube
```

## Step 2: Generate Secure Passwords

**CRITICAL**: Never use default or weak passwords. Generate strong random passwords:

```bash
# Generate JWT Secret (minimum 256 bits)
openssl rand -base64 64

# Generate Database Password
openssl rand -base64 32

# Generate Redis Password
openssl rand -base64 32

# Generate RabbitMQ Password
openssl rand -base64 32
```

## Step 3: Create Environment File

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` and fill in the required values (generated above):

```bash
# REQUIRED - Application will not start without these
JWT_SECRET=<paste generated 64-character secret>
DB_PASSWORD=<paste generated database password>
REDIS_PASSWORD=<paste generated redis password>
RABBITMQ_PASSWORD=<paste generated rabbitmq password>

# Optional OAuth2 Configuration (if using social login)
OAUTH_42_CLIENT_ID=<your 42 client ID>
OAUTH_42_CLIENT_SECRET=<your 42 client secret>
OAUTH_GOOGLE_CLIENT_ID=<your google client ID>
OAUTH_GOOGLE_CLIENT_SECRET=<your google client secret>
OAUTH_GITHUB_CLIENT_ID=<your github client ID>
OAUTH_GITHUB_CLIENT_SECRET=<your github client secret>

# Optional API Keys
TMDB_API_KEY=<your tmdb api key>
OPENSUBTITLES_API_KEY=<your opensubtitles api key>
```

## Step 4: Verify Security Configuration

Run the security check script (if available):

```bash
./scripts/check-security.sh
```

Or manually verify:

```bash
# Ensure no passwords in docker-compose.yml
grep -n "password" docker-compose.yml
# Should only show ${PASSWORD} references, no hardcoded values

# Ensure .env is in .gitignore
grep ".env" .gitignore
```

## Step 5: Start the Infrastructure

### Development Mode

For local development with debugging:

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

This mode:
- Enables DEBUG logging
- Disables Eureka self-preservation for faster feedback
- Allows empty Redis password (for local testing only)
- Exposes more actuator endpoints

**WARNING**: Never use dev profile in production!

### Production Mode

For production or production-like testing:

```bash
docker-compose up -d
```

This mode:
- INFO level logging only
- Eureka self-preservation enabled
- All passwords required
- Minimal actuator endpoints exposed

## Step 6: Verify Services

Check that all services started successfully:

```bash
# Check service status
docker-compose ps

# Check logs for startup errors
docker-compose logs api-gateway | grep "ERROR"
docker-compose logs api-gateway | grep "JWT configuration validated"

# Verify Eureka dashboard (should see all services registered)
curl http://localhost:8761
```

## Step 7: Test Security Configuration

### Test JWT Secret Validation

Try starting with an invalid JWT secret:

```bash
# This should FAIL with security error
JWT_SECRET="weak" docker-compose up api-gateway
# Expected: "SECURITY ERROR: JWT_SECRET is too short"
```

### Test Rate Limiting

```bash
# Rapid login attempts should be rate limited
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}'
  echo ""
done
# Expected: After 10 requests, you should see 429 Too Many Requests
```

### Test JWT Validation

```bash
# Request without token should fail
curl http://localhost:8080/api/v1/users/me
# Expected: 401 Unauthorized

# Request with invalid token should fail
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer invalid.token.here"
# Expected: 401 Unauthorized with "Invalid token" message
```

## Step 8: Production Deployment Checklist

Before deploying to production:

- [ ] All passwords are cryptographically random (not predictable)
- [ ] JWT_SECRET is at least 64 characters
- [ ] `.env` file is NOT committed to git
- [ ] `SPRING_PROFILES_ACTIVE=docker` (NOT dev)
- [ ] CORS origins updated in `SecurityConfig.java` to production URLs
- [ ] HTTPS/TLS configured for API Gateway
- [ ] Database backups configured
- [ ] Log aggregation configured
- [ ] Monitoring and alerting set up
- [ ] Security headers configured (CSP, HSTS, X-Frame-Options)
- [ ] Rate limiting thresholds reviewed for production traffic
- [ ] OAuth2 redirect URIs updated to production URLs

## Troubleshooting

### Application won't start - "JWT_SECRET is not set"

**Cause**: JWT_SECRET environment variable is missing.

**Solution**: Add JWT_SECRET to your `.env` file or export it:
```bash
export JWT_SECRET=$(openssl rand -base64 64)
```

### Application won't start - "JWT_SECRET is too short"

**Cause**: JWT_SECRET is less than 32 bytes (256 bits).

**Solution**: Generate a longer secret:
```bash
export JWT_SECRET=$(openssl rand -base64 64)
```

### Redis connection failed

**Cause**: Redis password not set or incorrect.

**Solution**: Verify REDIS_PASSWORD in `.env` matches Redis container configuration.

### PostgreSQL authentication failed

**Cause**: DB_PASSWORD not set or incorrect.

**Solution**: Verify DB_PASSWORD in `.env` and restart postgres container:
```bash
docker-compose restart postgres
```

### Rate limiting not working

**Cause**: Redis connection issue or rate limiter not configured.

**Solution**:
```bash
# Check Redis is running and accessible
docker-compose exec redis redis-cli -a "$REDIS_PASSWORD" ping
# Should return: PONG

# Check rate limiter logs
docker-compose logs api-gateway | grep -i "rate"
```

## Security Best Practices

### Never commit secrets to git

```bash
# Verify .env is ignored
git status
# .env should NOT appear in untracked files

# If accidentally committed:
git rm --cached .env
git commit -m "Remove .env from git"
```

### Rotate passwords periodically

```bash
# Generate new password
NEW_PASSWORD=$(openssl rand -base64 32)

# Update .env file
sed -i "s/DB_PASSWORD=.*/DB_PASSWORD=$NEW_PASSWORD/" .env

# Restart affected services
docker-compose restart postgres user-service search-service streaming-service comment-service
```

### Backup critical secrets securely

Store passwords in a secure password manager or secrets management system:
- HashiCorp Vault
- AWS Secrets Manager
- Azure Key Vault
- 1Password/LastPass (for smaller deployments)

**Never** store passwords in:
- Slack messages
- Email
- Plain text files in cloud storage
- Shared documents

## Support

For security questions or to report vulnerabilities:
- Review `/SECURITY.md` for detailed security documentation
- Review `/services/api-gateway/CSRF_PROTECTION.md` for CSRF decisions
- Contact the development team

**Last Updated**: 2025-11-25
