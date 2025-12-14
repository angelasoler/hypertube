# E2E Test Setup - Current Status (Updated 2025-12-13)

## 🎯 Quick Summary

**Login E2E Test**: ✅ **WORKING**
**Browse Page Test**: ⚠️ **BLOCKED** - Services currently stopped
**Last Run**: 6 hours ago - all containers stopped

---

## ✅ What We Fixed (Latest Session)

### 1. Frontend → API Gateway Communication ✅ FIXED
**Problem**: Frontend couldn't connect to backend API
**Root Causes**:
- Frontend configured with wrong API base URL (`localhost:8080` instead of Docker service name)
- Nuxt Nitro `devProxy` not working properly for API requests

**Solutions Applied**:
1. **docker-compose.yml** line 283:
   ```yaml
   environment:
     NUXT_PUBLIC_API_BASE: http://api-gateway:8080  # Changed from localhost
   ```

2. **Created** `frontend/server/middleware/api-proxy.ts`:
   ```typescript
   // Proper Nuxt server middleware for proxying /api/* requests
   export default defineEventHandler(async (event) => {
     if (event.path.startsWith('/api')) {
       const target = process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080'
       return proxyRequest(event, `${target}${path}`)
     }
   })
   ```

3. **Updated** `frontend/composables/useApi.ts` line 25:
   ```typescript
   // Use relative paths in browser, absolute on server
   const url = process.client ? endpoint : `${apiBase}${endpoint}`
   ```

**Test Result**: ✅ Login via curl returns JWT tokens successfully

### 2. Test User Created ✅
**Username**: `e2e_user`
**Password**: `TestPass123`
**Database**: hypertube-postgres
**Status**: Verified working via API

### 3. Test Selectors Added ✅
**Files Modified**:
- `frontend/components/VideoFilters.vue`: Added `data-testid="search-input"`
- `frontend/components/VideoCard.vue`: Added `data-testid="video-card"`

### 4. Debug Test Created ✅
**File**: `tests/e2e/specs/simple-login.spec.ts`
**Purpose**: Logs console output, network requests, and errors
**Result**: Login successful, redirect to `/` working

---

## 📦 Current Services Status

**All services are STOPPED** (stopped 6 hours ago):
```
hypertube-eureka          - Exited (143) 6 hours ago
hypertube-frontend        - Exited (0) 6 hours ago
hypertube-gateway         - Exited (137) 6 hours ago
hypertube-postgres        - Exited (0) 6 hours ago
hypertube-rabbitmq        - Exited (0) 6 hours ago
hypertube-redis           - Exited (0) 6 hours ago
hypertube-user-service    - Exited (143) 6 hours ago
hypertube-search-service  - Exited (255) 8 hours ago
hypertube-streaming-service - Exited (255) 8 hours ago
```

---

## 🚀 How to Run E2E Tests (Step-by-Step)

### Method 1: Using Makefile (Recommended)

```bash
# Step 1: Start all essential services
make up

# Step 2: Wait for services to be healthy (~60 seconds)
make health

# Step 3: Build E2E test Docker image
cd tests/e2e && docker build -t hypertube-e2e-tests . && cd ../..

# Step 4: Run login test
docker run --rm --network host hypertube-e2e-tests \
  npx playwright test video-search.spec.ts:16 --reporter=line
```

### Method 2: Manual Docker Compose

```bash
# Step 1: Start only working services (skip comment-service and streaming-worker)
docker compose up -d \
  postgres \
  redis \
  rabbitmq \
  eureka-server \
  api-gateway \
  user-service \
  search-service \
  streaming-service \
  frontend

# Step 2: Check status
docker compose ps

# Step 3: Wait for healthy status
# Watch logs: docker compose logs -f api-gateway user-service frontend

# Step 4: Verify services manually
curl http://localhost:8761/  # Eureka
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:3000/  # Frontend

# Step 5: Test login
curl -X POST -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"e2e_user","password":"TestPass123"}' \
  http://localhost:3000/api/auth/login

# Step 6: Run E2E tests
docker run --rm --network host hypertube-e2e-tests \
  npx playwright test simple-login.spec.ts --reporter=line
```

---

## 🏗️ Service Architecture

### Essential Services (Required for Login Tests)
| Service | Port | Container Name | Purpose |
|---------|------|----------------|---------|
| postgres | 5432 | hypertube-postgres | Database |
| redis | 6379 | hypertube-redis | Cache |
| rabbitmq | 5672, 15672 | hypertube-rabbitmq | Message queue |
| eureka-server | 8761 | hypertube-eureka | Service discovery |
| api-gateway | 8080 | hypertube-gateway | API Gateway |
| user-service | - | hypertube-user-service | Auth & users |
| frontend | 3000 | hypertube-frontend | Nuxt 3 app |

### Additional Services (For Browse/Search Tests)
| Service | Purpose | Status |
|---------|---------|--------|
| search-service | Video search API | ⚠️ Not tested |
| streaming-service | Video streaming | ⚠️ Not tested |

### Broken/Incomplete Services (Don't Start)
| Service | Issue |
|---------|-------|
| comment-service | Empty directory - no Dockerfile |
| streaming-worker | May have build issues |

---

## 🧪 Test Results

### ✅ Passing Tests
1. **Simple Login Test**
   - File: `tests/e2e/specs/simple-login.spec.ts`
   - Result: ✅ PASS (1 passed in 37.1s)
   - Details:
     - POST `/api/auth/login` → 200 OK
     - Returns valid JWT token
     - Redirects to `/` (homepage)

### ⚠️ Tests Not Run (Services Stopped)
1. **Video Search Test** - Requires running services
2. **Browse Page Navigation** - Requires search-service

---

## 📁 Important Files Modified

### Docker Configuration
- `docker-compose.yml` (line 283) - Frontend API base URL

### Frontend Code
- `frontend/server/middleware/api-proxy.ts` - **NEW FILE** - API proxy
- `frontend/composables/useApi.ts` - Client/server URL handling
- `frontend/components/VideoFilters.vue` - Added `data-testid`
- `frontend/components/VideoCard.vue` - Added `data-testid`

### Test Files
- `tests/e2e/specs/simple-login.spec.ts` - **NEW FILE** - Debug test
- `tests/e2e/fixtures/test-data.json` - Updated password

---

## 🔧 Makefile Commands

The project has a comprehensive Makefile. Key commands:

```bash
# Service Management
make help              # Show all commands
make up                # Start all services
make down              # Stop all services
make restart           # Restart all services
make ps                # Show service status
make health            # Check service health

# Infrastructure Only
make start-infra       # Start postgres, redis, rabbitmq only
make start-eureka      # Start Eureka
make start-gateway     # Start API Gateway

# Logs
make logs              # All logs
make logs-gateway      # API Gateway logs only
make logs-frontend     # Frontend logs only

# Development
make up-dev            # Start with dev profile
make build             # Build all images
make build-no-cache    # Build without cache

# Cleanup
make down-volumes      # Stop and delete data
make clean             # Clean build artifacts
make clean-docker      # Remove images
```

---

## ⚠️ Known Issues & Workarounds

### Issue 1: Comment Service Missing
**Error**: `failed to read dockerfile: open Dockerfile: no such file or directory`

**Workaround**: Don't start comment-service and streaming-worker:
```bash
docker compose up -d postgres redis rabbitmq eureka-server api-gateway user-service search-service streaming-service frontend
```

### Issue 2: Services Take Time to Start
**Problem**: Eureka registration takes 30-60 seconds

**Solution**: Wait before running tests:
```bash
make up
sleep 60  # Wait for services to register
make health  # Verify
```

### Issue 3: Frontend "Unhealthy" Status
**Observation**: Frontend marked as "unhealthy" but works fine

**Explanation**: Health check may be too strict, but the app responds normally

**Verification**:
```bash
curl http://localhost:3000/  # Should return HTML
```

---

## 📋 User Action Checklist

Execute these steps to verify everything works:

- [ ] **Step 1**: Start services
  ```bash
  make up
  # OR
  docker compose up -d postgres redis rabbitmq eureka-server api-gateway user-service frontend
  ```

- [ ] **Step 2**: Wait 60 seconds and check status
  ```bash
  sleep 60
  docker compose ps
  ```

- [ ] **Step 3**: Verify Eureka has registered services
  ```bash
  curl -s http://localhost:8761/eureka/apps | grep -E "<name>"
  # Should show: API-GATEWAY, USER-MANAGEMENT-SERVICE
  ```

- [ ] **Step 4**: Test API Gateway health
  ```bash
  curl http://localhost:8080/actuator/health
  # Should return: {"status":"UP"}
  ```

- [ ] **Step 5**: Test frontend is running
  ```bash
  curl -I http://localhost:3000/
  # Should return: HTTP/1.1 200 or 302
  ```

- [ ] **Step 6**: Test login via frontend proxy
  ```bash
  curl -X POST -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"e2e_user","password":"TestPass123"}' \
    http://localhost:3000/api/auth/login
  # Should return: JSON with accessToken
  ```

- [ ] **Step 7**: Run simple login E2E test
  ```bash
  docker run --rm --network host \
    -v $(pwd)/tests/e2e/specs/simple-login.spec.ts:/app/e2e/specs/simple-login.spec.ts \
    hypertube-e2e-tests \
    npx playwright test simple-login.spec.ts --reporter=line
  # Should return: 1 passed
  ```

- [ ] **Step 8**: (Optional) Test browse page
  ```bash
  curl http://localhost:3000/browse
  # Requires search-service running
  ```

---

## 🎯 Next Goals

Once you've verified the checklist above, we can:

1. **Test Browse Page** - Requires search-service to be running and healthy
2. **Test Video Search** - Full E2E flow with video results
3. **Test Video Streaming** - Download and playback functionality
4. **Add More E2E Tests** - Cover edge cases and error scenarios

---

## 📞 Troubleshooting

### Services Won't Start
```bash
# Check Docker daemon
docker ps

# Check logs for errors
docker compose logs api-gateway
docker compose logs user-service

# Clean restart
make down
make clean-docker
make up
```

### API Gateway Returns 401
```bash
# Verify route configuration
docker exec hypertube-gateway cat /app/application.yml | grep -A 10 "routes:"

# Check Eureka registration
curl http://localhost:8761/eureka/apps
```

### Frontend Can't Connect to Backend
```bash
# Check environment variable
docker exec hypertube-frontend printenv NUXT_PUBLIC_API_BASE
# Should show: http://api-gateway:8080

# Check proxy middleware exists
docker exec hypertube-frontend ls -la /app/server/middleware/
# Should show: api-proxy.ts
```

### E2E Tests Fail
```bash
# Check if services are accessible from host
curl http://localhost:8080/actuator/health
curl http://localhost:3000/

# Run test with verbose output
docker run --rm --network host hypertube-e2e-tests \
  npx playwright test --debug simple-login.spec.ts
```

---

## 📊 Progress Summary

| Component | Status | Notes |
|-----------|--------|-------|
| Frontend API Proxy | ✅ Fixed | Server middleware working |
| User Authentication | ✅ Working | JWT tokens generated |
| Test User Created | ✅ Done | `e2e_user` in database |
| Test Selectors | ✅ Added | data-testid attributes |
| Login E2E Test | ✅ Passing | 1 passed in 37s |
| Browse Page Test | ⏳ Pending | Need running services |
| Video Search Test | ⏳ Pending | Need search-service |
| Full E2E Suite | ⏳ Pending | Need all services |

---

## 📝 Summary for Future Sessions

**What works**:
- ✅ Frontend → API Gateway proxy via Nuxt middleware
- ✅ User login through frontend
- ✅ JWT token generation
- ✅ E2E test infrastructure

**What to do next time**:
1. Run `make up` or start services manually (skip comment-service)
2. Wait 60 seconds for service registration
3. Run `make health` to verify
4. Execute E2E tests with `docker run --network host`

**Don't forget**:
- Services need time to register with Eureka
- Frontend proxy requires api-gateway Docker service name
- comment-service and streaming-worker are incomplete - don't start them
