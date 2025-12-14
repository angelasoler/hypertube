# HyperTube E2E Tests

End-to-end tests for the HyperTube video streaming platform using Playwright and Firefox.

## Overview

This test suite validates the complete user journey from video search to streaming playback, including:

- Video search functionality
- Search filters and sorting
- Video player integration
- BitTorrent download progress
- HTTP Range request handling
- Subtitle integration
- Video seeking and controls

## Prerequisites

- Node.js 18+ installed
- Docker and Docker Compose running
- HyperTube services running (frontend + backend)
- Test user account created

## Quick Start

### 1. Install Dependencies

```bash
cd tests
npm install
```

### 2. Install Playwright Firefox

```bash
npx playwright install firefox
```

### 3. Start Services

Make sure all HyperTube services are running:

```bash
cd ..
docker-compose up -d
```

Wait for services to be healthy:

```bash
# Check frontend
curl http://localhost:3000

# Check backend
curl http://localhost:8080/actuator/health
```

### 4. Create Test User

Run this once to create the test user:

```bash
./setup-test-user.sh
```

Or manually:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "e2e_test_user",
    "email": "e2e@test.com",
    "password": "Test123!@#",
    "firstName": "E2E",
    "lastName": "Test User"
  }'
```

### 5. Run Tests

```bash
# Run all tests
npm test

# Run specific test suite
npm run test:search
npm run test:streaming
npm run test:complete

# Run in debug mode
npm run test:debug

# Run with UI
npm run test:ui
```

## Test Suites

### 1. Video Search Tests (`video-search.spec.ts`)

Tests search functionality including:
- Search form display
- Search results display
- Empty search handling
- Navigation to video page
- Genre filters
- Sorting by rating
- Infinite scroll pagination
- URL query preservation
- Thumbnail display

**Duration**: ~2-3 minutes

### 2. Video Streaming Tests (`video-streaming.spec.ts`)

Tests streaming functionality including:
- HTTP Range requests
- Network error handling
- Video resolution verification
- Subtitle track handling
- Download progress updates
- Download speed and ETA display
- Video caching
- Multiple seek operations
- Video controls

**Duration**: ~5-10 minutes

### 3. Complete Flow Tests (`video-complete-flow.spec.ts`)

Tests the complete user journey:
1. Search for video
2. Select video from results
3. Navigate to watch page
4. Wait for download to start
5. Monitor progress to 10% buffer
6. Verify video player appears
7. Verify video plays
8. Test seeking
9. Test subtitles
10. Verify download stats

**Duration**: ~3-5 minutes per test

## Test Configuration

### Environment Variables

Create `.env.test` in the tests directory:

```env
FRONTEND_URL=http://localhost:3000
API_URL=http://localhost:8080
DATABASE_URL=postgresql://test:test@localhost:5432/hypertube_test
REDIS_URL=redis://localhost:6379/1
RABBITMQ_URL=amqp://localhost:5672
```

### Playwright Config

Configuration is in [`playwright.config.ts`](./playwright.config.ts):

- **Timeout**: 2 minutes per test (video downloads can be slow)
- **Workers**: 1 (sequential execution)
- **Retries**: 2 on CI, 0 locally
- **Browser**: Firefox Desktop
- **Video recording**: On failure
- **Screenshots**: On failure
- **Traces**: On first retry

## Test Data

Test data is defined in [`e2e/fixtures/test-data.json`](./e2e/fixtures/test-data.json):

- **Test Videos**: Open-source videos (Big Buck Bunny, Sintel, Elephants Dream)
- **Test User**: Credentials for authentication
- **Search Queries**: Popular and specific queries
- **Filters**: Genre, year, rating options

## Helpers

### API Helper ([`e2e/helpers/api-helper.ts`](./e2e/helpers/api-helper.ts))

Provides methods for API interactions:
- `login(username, password)` - Authenticate user
- `register(userData)` - Register new user
- `searchVideos(query, page, filters)` - Search for videos
- `getVideoDetails(videoId)` - Get video metadata
- `initiateDownload(data)` - Start video download
- `checkDownloadProgress(jobId)` - Check download status
- `getSubtitles(videoId)` - Get available subtitles

### Video Helper ([`e2e/helpers/video-helper.ts`](./e2e/helpers/video-helper.ts))

Provides methods for video player interactions:
- `waitForVideoPlayer()` - Wait for video element
- `isVideoPlaying()` - Check if video is playing
- `getVideoCurrentTime()` - Get current playback time
- `seekTo(time)` - Seek to specific time
- `play()` / `pause()` - Control playback
- `selectSubtitle(language)` - Select subtitle track
- `getAvailableSubtitles()` - List subtitle languages
- `waitForDownloadProgress(percent)` - Wait for download threshold
- `getVideoStats()` - Get video resolution and state

## Debugging

### View Test Report

After running tests:

```bash
npm run report
```

### View Trace

If a test fails and produces a trace:

```bash
npx playwright show-trace trace.zip
```

### Debug Mode

Run tests with Playwright Inspector:

```bash
npm run test:debug
```

### UI Mode

Run tests with interactive UI:

```bash
npm run test:ui
```

### Console Logs

Tests output detailed console logs showing:
- Current step being executed
- Progress percentages
- Video metadata
- Download statistics
- Errors and warnings

### Screenshots and Videos

Failed tests automatically capture:
- Screenshots (in `test-results/`)
- Video recordings (in `test-results/`)
- Traces (in `test-results/`)

## Common Issues

### 1. Services Not Running

**Error**: `ECONNREFUSED` or timeout

**Solution**:
```bash
docker-compose up -d
docker-compose ps  # Verify all services are up
```

### 2. Test User Doesn't Exist

**Error**: Login fails with 401

**Solution**:
```bash
./setup-test-user.sh
```

### 3. Video Download Timeout

**Error**: `Download did not reach 10% within timeout`

**Solution**:
- Check torrent service is running: `docker logs hypertube-streaming-service`
- Verify network connectivity
- Increase timeout in test
- Use smaller test video

### 4. Video Player Not Appearing

**Error**: `Video player not found`

**Solution**:
- Check download progress is updating
- Verify streaming service logs
- Check for JavaScript errors in browser console
- Ensure video format is supported (MP4)

### 5. Subtitles Not Loading

**Error**: No subtitles available

**Solution**:
- Not all videos have subtitles
- Check subtitle service logs
- Verify subtitle files were downloaded
- Check OpenSubtitles API status

## CI/CD Integration

### GitHub Actions

Example workflow (`.github/workflows/e2e-tests.yml`):

```yaml
name: E2E Tests

on:
  push:
    branches: [ main, feature/* ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Install dependencies
        run: cd tests && npm ci

      - name: Start services
        run: docker-compose up -d

      - name: Wait for services
        run: |
          timeout 120 bash -c 'until curl -f http://localhost:3000; do sleep 5; done'
          timeout 120 bash -c 'until curl -f http://localhost:8080/actuator/health; do sleep 5; done'

      - name: Install Playwright
        run: cd tests && npx playwright install --with-deps firefox

      - name: Run tests
        run: cd tests && npm test

      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: tests/playwright-report/
```

## Writing New Tests

### Test Structure

```typescript
import { test, expect } from '@playwright/test';
import { VideoHelper } from '../helpers/video-helper';
import testData from '../fixtures/test-data.json';

test.describe('My Test Suite', () => {
  test.beforeEach(async ({ page }) => {
    // Login or setup
  });

  test('should do something', async ({ page }) => {
    // Arrange
    await page.goto('/some-page');

    // Act
    await page.click('button');

    // Assert
    await expect(page.locator('.result')).toBeVisible();
  });
});
```

### Best Practices

1. **Use data-testid attributes** for reliable selectors
2. **Add console logs** for debugging
3. **Handle timeouts gracefully** - video operations can be slow
4. **Check element existence** before interacting
5. **Wait for network requests** to complete
6. **Use helpers** for common operations
7. **Keep tests independent** - don't rely on previous test state
8. **Clean up after tests** if needed

### Example: Adding a New Test

```typescript
test('should verify video quality settings', async ({ page }) => {
  const videoHelper = new VideoHelper(page);

  // Navigate to video
  await page.goto('/watch/test-video-id');

  // Wait for player
  await videoHelper.waitForVideoPlayer();

  // Get video stats
  const stats = await videoHelper.getVideoStats();

  // Assert
  expect(stats.width).toBeGreaterThan(1280); // At least 720p
  expect(stats.height).toBeGreaterThan(720);
});
```

## Performance Benchmarks

Expected performance for E2E tests:

| Operation | Expected Time | Timeout |
|-----------|---------------|---------|
| Search results | < 10s | 30s |
| Video page load | < 5s | 10s |
| Download start | < 10s | 30s |
| 10% buffer (100MB) | 30-120s | 180s |
| Video player appear | < 5s | 60s |
| Seeking | < 3s | 10s |
| Subtitle load | < 2s | 10s |

## Maintenance

### Update Test Data

Edit [`e2e/fixtures/test-data.json`](./e2e/fixtures/test-data.json) to:
- Add new test videos
- Update search queries
- Modify filter options

### Update Selectors

If UI changes, update selectors in:
- Test specs (`*.spec.ts`)
- Helper methods
- Consider using `data-testid` attributes

### Update Timeouts

If tests are flaky due to slow operations:
1. Identify slow operation in logs
2. Increase specific timeout (not global)
3. Consider optimizing the operation itself

## Resources

- [Playwright Documentation](https://playwright.dev/docs/intro)
- [E2E Testing Plan](../E2E_TESTING_PLAN.md)
- [Video Streaming Integration Guide](../VIDEO_STREAMING_INTEGRATION.md)
- [HyperTube Project README](../README.md)

## Support

If you encounter issues with the tests:

1. Check logs: `docker-compose logs`
2. Verify services health: `docker-compose ps`
3. Review test output and screenshots
4. Check browser console for errors
5. Run with debug mode: `npm run test:debug`

## License

Same as the main HyperTube project.
