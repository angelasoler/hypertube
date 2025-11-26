# Security Documentation

This document describes the security measures implemented in HyperTube infrastructure.

## Critical Security Requirements

All security requirements from CLAUDE.md are implemented and enforced:

### 1. Password Security
- **Status**: ✅ ENFORCED
- No plain text passwords in configuration files
- All passwords required via environment variables (no defaults)
- Application fails to start if required passwords are not set
- Recommendation: Use `openssl rand -base64 32` to generate strong passwords

### 2. JWT Authentication Security
- **Status**: ✅ ENFORCED
- JWT secret required via environment variable (minimum 256 bits)
- Startup validation prevents application launch with weak/default secrets
- Comprehensive JWT validation:
  - Signature verification
  - Expiration checking
  - Issuer validation (`hypertube`)
  - Audience validation (`hypertube-api`)
  - Not-before time validation
- Claim sanitization to prevent injection attacks
- See: `/services/api-gateway/src/main/java/com/hypertube/gateway/config/JwtConfigValidator.java`

### 3. Input Validation
- **Status**: ✅ IMPLEMENTED
- All JWT claims sanitized before forwarding to downstream services
- Control character removal
- Header injection prevention (newline/carriage return stripping)
- Length limits to prevent DOS attacks
- See: `JwtAuthenticationFilter.sanitizeClaimValue()`

### 4. Rate Limiting
- **Status**: ✅ IMPLEMENTED (Tiered Strategy)

**Authentication Endpoints** (Strict):
- Login: 3 req/sec, burst: 10
- Register: 2 req/sec, burst: 5
- Password Reset: 1 req/sec, burst: 3
- OAuth: 5 req/sec, burst: 15

**General API Endpoints** (Standard):
- User Profile: 10 req/sec, burst: 30
- Video Search: 20 req/sec, burst: 50 (higher for browsing)
- Streaming: 15 req/sec, burst: 40
- Comments: 10 req/sec, burst: 25

**Implementation**:
- IP-based rate limiting for unauthenticated requests
- User-based rate limiting for authenticated requests
- Redis-backed for distributed rate limiting
- See: `/services/api-gateway/src/main/java/com/hypertube/gateway/config/RateLimitConfig.java`

### 5. CSRF Protection
- **Status**: ✅ DISABLED (Appropriately)
- Reason: Stateless API with JWT in Authorization headers (not cookies)
- Alternative protections: CORS, JWT issuer/audience validation, rate limiting
- See: `/services/api-gateway/CSRF_PROTECTION.md` for detailed justification

### 6. SQL Injection Prevention
- **Status**: ✅ ARCHITECTURE
- Spring Data JPA/Hibernate ORM used throughout
- Parameterized queries enforced
- No raw SQL concatenation

### 7. XSS Prevention
- **Status**: ✅ IMPLEMENTED
- Input sanitization in JWT filter
- Frontend must implement output encoding (Vue.js default behavior)
- Content-Type validation for uploads (to be implemented in streaming service)

## Infrastructure Security

### Database (PostgreSQL)
- **Password**: Required via `DB_PASSWORD` environment variable (no default)
- **Network**: Isolated to `hypertube-network` Docker network
- **Access**: Only accessible by backend services (not exposed publicly in production)

### Redis
- **Password**: Required via `REDIS_PASSWORD` environment variable (no default)
- **Connection Pool**: Configured with limits to prevent resource exhaustion
  - Max active connections: 20
  - Max idle: 10
  - Min idle: 5
  - Connection timeout: 3 seconds
- **Network**: Isolated to `hypertube-network` Docker network
- **Command**: `requirepass` enforced via Docker command

### RabbitMQ
- **Password**: Required via `RABBITMQ_PASSWORD` environment variable (no default)
- **Management UI**: Exposed only in development (port 15672)
- **Network**: Isolated to `hypertube-network` Docker network

### Eureka Service Discovery
- **Self-Preservation**: Enabled in production, disabled in dev profile
  - Production: Prevents mass de-registration during network issues
  - Development: Disabled for faster feedback during testing
- **Configuration**: Profile-based (`application.yml` vs `application-dev.yml`)

## Logging Security

### Production Logging
- **Level**: INFO for all components
- **Sensitive Data**: No passwords, tokens, or user data logged at INFO level
- **Files**:
  - `/services/api-gateway/src/main/resources/application.yml`
  - `/services/api-gateway/src/main/resources/application-docker.yml`
  - `/services/eureka-server/src/main/resources/application.yml`

### Development Logging
- **Level**: DEBUG for troubleshooting
- **WARNING**: Development profile should NEVER be used in production
- **Files**:
  - `/services/api-gateway/src/main/resources/application-dev.yml`
  - `/services/eureka-server/src/main/resources/application-dev.yml`

## Environment Variables

### Required (Application fails without these)
```bash
# JWT Configuration (CRITICAL)
JWT_SECRET=          # Min 256 bits, generate with: openssl rand -base64 64

# Database
DB_PASSWORD=         # Generate with: openssl rand -base64 32

# Redis
REDIS_PASSWORD=      # Generate with: openssl rand -base64 32

# RabbitMQ
RABBITMQ_PASSWORD=   # Generate with: openssl rand -base64 32
```

### Optional (Have defaults)
```bash
JWT_ISSUER=hypertube
JWT_AUDIENCE=hypertube-api
JWT_ACCESS_TOKEN_EXPIRY=3600
DB_NAME=hypertube
DB_USER=hypertube_user
RABBITMQ_USER=hypertube
```

## Security Checklist for Deployment

Before deploying to production:

- [x] Generate strong random password for `JWT_SECRET` (min 64 characters)
- [x] Generate strong random password for `DB_PASSWORD`
- [x] Generate strong random password for `REDIS_PASSWORD`
- [x] Generate strong random password for `RABBITMQ_PASSWORD`
- [ ] Set all OAuth2 client IDs and secrets
- [ ] Verify `SPRING_PROFILES_ACTIVE=docker` (NOT dev)
- [ ] Review CORS allowed origins in `SecurityConfig.java`
- [ ] Enable HTTPS/TLS for API Gateway in production
- [ ] Configure proper firewall rules (only ports 80, 443, and 3000 exposed)
- [ ] Review rate limiting thresholds for production traffic patterns
- [ ] Set up log aggregation and monitoring
- [ ] Configure backup strategy for PostgreSQL
- [x] Review and update `.env` file (never commit to git)
- [ ] Verify Docker network isolation

## OAuth2 Security

OAuth2 providers configured:
1. **42 School** (Mandatory)
2. **Google** (Optional)
3. **GitHub** (Optional)

All OAuth2 implementations must:
- Use state parameter to prevent CSRF
- Validate redirect URIs
- Use HTTPS in production
- Store tokens securely (never in localStorage for refresh tokens)

## Monitoring and Alerting

Recommended monitoring:
- Failed authentication attempts (rate limit violations)
- JWT validation failures
- Redis connection pool exhaustion
- Database connection failures
- Unusual rate limiting patterns
- Service health checks

## Security Audit Trail

| Date | Issue | Resolution | Reviewer |
|------|-------|-----------|----------|
| 2025-11-25 | Weak JWT default secret | Removed default, added startup validation | Technical Debt Specialist |
| 2025-11-25 | Incomplete JWT validation | Added issuer/audience/expiration checks | Technical Debt Specialist |
| 2025-11-25 | Missing input sanitization | Added claim sanitization in JWT filter | Technical Debt Specialist |
| 2025-11-25 | Weak database passwords | Removed all password defaults | Technical Debt Specialist |
| 2025-11-25 | No Redis connection pooling | Added Lettuce pool configuration | Technical Debt Specialist |
| 2025-11-25 | Inadequate rate limiting | Implemented tiered strategy | Technical Debt Specialist |
| 2025-11-25 | Eureka self-preservation | Enabled for production, disabled for dev | Technical Debt Specialist |
| 2025-11-25 | DEBUG logging in production | Moved to dev profile only | Technical Debt Specialist |
| 2025-11-25 | Empty Redis password default | Removed default, required via env var | Technical Debt Specialist |
| 2025-11-25 | CSRF protection undocumented | Created CSRF_PROTECTION.md | Technical Debt Specialist |

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Project Security Requirements](./CLAUDE.md#security-requirements)

## Contact

For security concerns or to report vulnerabilities, contact the development team.

**Last Updated**: 2025-11-25
**Next Review**: 2026-02-25 (Quarterly)
