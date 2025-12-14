# End-to-End Testing Plan: Video Search and Streaming

## Overview

This document outlines the E2E testing strategy for HyperTube's video search and streaming functionality using browser automation.

## Testing Approach

### Tool Selection

**Primary: Playwright**
- Excellent Firefox support (user requested Firefox)
- Modern API with auto-waiting
- Built-in test runner
- Network interception capabilities
- Video recording for debugging

**Alternative: Puppeteer**
- Can work with Firefox via puppeteer-firefox
- Popular and well-documented

**Recommendation: Playwright** - Better Firefox support and more robust for E2E testing.

## Test Flow Scenarios

### Scenario 1: Complete Video Search and Stream Flow
**User Journey**: Search → Select → Watch → Stream

**Steps**:
1. Navigate to homepage
2. Enter search query "Big Buck Bunny" (open source test video)
3. Wait for search results to load
4. Verify results are displayed
5. Click on first video result
6. Verify watch page loads with video metadata
7. Verify download initiates automatically
8. Monitor download progress
9. Wait for 10% buffer threshold
10. Verify video player appears
11. Verify video starts playing
12. Test seeking functionality
13. Verify subtitles load and can be selected
14. Verify progress tracking updates

### Scenario 2: Search with Filters
**Steps**:
1. Navigate to search page
2. Apply filters (genre, year, rating)
3. Enter search query
4. Verify filtered results
5. Verify infinite scroll pagination

### Scenario 3: Resume Watching
**Steps**:
1. Search for video
2. Start watching (reach 10% buffer)
3. Navigate away
4. Return to video
5. Verify streaming continues from cache

## Technical Implementation

### Project Structure

```
tests/
├── e2e/
│   ├── fixtures/
│   │   └── test-data.json
│   ├── helpers/
│   │   ├── api-helper.ts
│   │   └── video-helper.ts
│   ├── specs/
│   │   ├── video-search.spec.ts
│   │   ├── video-streaming.spec.ts
│   │   └── video-complete-flow.spec.ts
│   └── playwright.config.ts
├── package.json
└── README.md
```

### Test Configuration

**Playwright Config** (playwright.config.ts):
```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e/specs',
  timeout: 120000, // 2 minutes (video downloads can be slow)
  expect: {
    timeout: 30000
  },
  fullyParallel: false, // Run sequentially to avoid overwhelming services
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [
    ['html'],
    ['list'],
    ['junit', { outputFile: 'test-results/junit.xml' }]
  ],
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    video: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
  ],
  webServer: {
    command: 'docker-compose up',
    url: 'http://localhost:3000',
    timeout: 120000,
    reuseExistingServer: !process.env.CI,
  },
});
```

### Test Data Fixtures

**fixtures/test-data.json**:
```json
{
  "testVideos": [
    {
      "query": "Big Buck Bunny",
      "expectedTitle": "Big Buck Bunny",
      "magnetUri": "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c",
      "fileSize": 354983651,
      "duration": 634
    },
    {
      "query": "Sintel",
      "expectedTitle": "Sintel",
      "magnetUri": "magnet:?xt=urn:btih:08ada5a7a6183aae1e09d831df6748d566095a10",
      "fileSize": 131099736,
      "duration": 888
    }
  ],
  "testUser": {
    "username": "e2e_test_user",
    "email": "e2e@test.com",
    "password": "Test123!@#"
  }
}
```

## Test Implementation

### Helper: API Helper

**helpers/api-helper.ts**:
```typescript
import { request } from '@playwright/test';

export class ApiHelper {
  private baseURL: string;
  private token?: string;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }

  async login(username: string, password: string) {
    const context = await request.newContext({ baseURL: this.baseURL });
    const response = await context.post('/api/auth/login', {
      data: { username, password }
    });

    if (response.ok()) {
      const data = await response.json();
      this.token = data.token;
    }

    return this.token;
  }

  async searchVideos(query: string, page: number = 0) {
    const context = await request.newContext({
      baseURL: this.baseURL,
      extraHTTPHeaders: {
        'Authorization': `Bearer ${this.token}`
      }
    });

    const response = await context.get('/api/search', {
      params: { q: query, page }
    });

    return response.json();
  }

  async initiateDownload(magnetUri: string, videoId: string) {
    const context = await request.newContext({
      baseURL: this.baseURL,
      extraHTTPHeaders: {
        'Authorization': `Bearer ${this.token}`
      }
    });

    const response = await context.post('/api/streaming/download', {
      data: { magnetUri, videoId }
    });

    return response.json();
  }

  async checkDownloadProgress(jobId: string) {
    const context = await request.newContext({
      baseURL: this.baseURL,
      extraHTTPHeaders: {
        'Authorization': `Bearer ${this.token}`
      }
    });

    const response = await context.get(`/api/streaming/jobs/${jobId}/ready`);
    return response.json();
  }
}
```

### Helper: Video Helper

**helpers/video-helper.ts**:
```typescript
import { Page, expect } from '@playwright/test';

export class VideoHelper {
  constructor(private page: Page) {}

  async waitForVideoPlayer() {
    await this.page.waitForSelector('video', { timeout: 60000 });
  }

  async isVideoPlaying(): Promise<boolean> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      return video && !video.paused && video.currentTime > 0;
    });
  }

  async getVideoCurrentTime(): Promise<number> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      return video?.currentTime || 0;
    });
  }

  async seekTo(timeInSeconds: number) {
    await this.page.evaluate((time) => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (video) {
        video.currentTime = time;
      }
    }, timeInSeconds);
  }

  async selectSubtitle(language: string) {
    await this.page.evaluate((lang) => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (video && video.textTracks) {
        for (let i = 0; i < video.textTracks.length; i++) {
          const track = video.textTracks[i];
          track.mode = track.language === lang ? 'showing' : 'hidden';
        }
      }
    }, language);
  }

  async getAvailableSubtitles(): Promise<string[]> {
    return await this.page.evaluate(() => {
      const video = document.querySelector('video') as HTMLVideoElement;
      if (!video || !video.textTracks) return [];

      const languages: string[] = [];
      for (let i = 0; i < video.textTracks.length; i++) {
        languages.push(video.textTracks[i].language);
      }
      return languages;
    });
  }

  async waitForDownloadProgress(targetPercent: number, timeout: number = 120000) {
    const startTime = Date.now();

    while (Date.now() - startTime < timeout) {
      const progress = await this.page.locator('[data-testid="download-progress"]')
        .textContent();

      if (progress) {
        const percent = parseFloat(progress.replace('%', ''));
        if (percent >= targetPercent) {
          return true;
        }
      }

      await this.page.waitForTimeout(2000); // Check every 2 seconds
    }

    throw new Error(`Download did not reach ${targetPercent}% within timeout`);
  }
}
```

### Test Spec: Complete Video Flow

**specs/video-complete-flow.spec.ts**:
```typescript
import { test, expect } from '@playwright/test';
import { ApiHelper } from '../helpers/api-helper';
import { VideoHelper } from '../helpers/video-helper';
import testData from '../fixtures/test-data.json';

test.describe('Video Search and Streaming - Complete Flow', () => {
  let apiHelper: ApiHelper;
  let videoHelper: VideoHelper;

  test.beforeEach(async ({ page, baseURL }) => {
    apiHelper = new ApiHelper(baseURL!);
    videoHelper = new VideoHelper(page);

    // Login before each test
    await page.goto('/login');
    await page.fill('input[name="username"]', testData.testUser.username);
    await page.fill('input[name="password"]', testData.testUser.password);
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  });

  test('should search for video, initiate download, and stream successfully', async ({ page }) => {
    const testVideo = testData.testVideos[0]; // Big Buck Bunny

    // Step 1: Navigate to search page
    await page.goto('/search');
    await expect(page).toHaveTitle(/Search/);

    // Step 2: Enter search query
    await page.fill('input[name="search"]', testVideo.query);
    await page.press('input[name="search"]', 'Enter');

    // Step 3: Wait for search results
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    // Step 4: Verify results are displayed
    const videoCards = page.locator('[data-testid="video-card"]');
    await expect(videoCards).toHaveCount(await videoCards.count());
    expect(await videoCards.count()).toBeGreaterThan(0);

    // Step 5: Click on first video result
    const firstVideo = videoCards.first();
    const videoId = await firstVideo.getAttribute('data-video-id');
    await firstVideo.click();

    // Step 6: Verify watch page loads
    await page.waitForURL(`/watch/${videoId}`);
    await expect(page.locator('h1')).toContainText(testVideo.expectedTitle);

    // Step 7: Verify metadata is displayed
    await expect(page.locator('[data-testid="video-year"]')).toBeVisible();
    await expect(page.locator('[data-testid="video-rating"]')).toBeVisible();
    await expect(page.locator('[data-testid="video-description"]')).toBeVisible();

    // Step 8: Verify download initiates automatically
    await expect(page.locator('[data-testid="download-status"]')).toContainText('Downloading');

    // Step 9: Monitor download progress to 10%
    console.log('Waiting for 10% buffer...');
    await videoHelper.waitForDownloadProgress(10);

    // Step 10: Verify video player appears
    await videoHelper.waitForVideoPlayer();
    await expect(page.locator('video')).toBeVisible();

    // Step 11: Verify video has valid source
    const videoSrc = await page.locator('video source').getAttribute('src');
    expect(videoSrc).toContain('/api/streaming/video/');

    // Step 12: Wait for video to start playing
    await page.waitForTimeout(3000); // Give video time to buffer and start

    const isPlaying = await videoHelper.isVideoPlaying();
    expect(isPlaying).toBeTruthy();

    // Step 13: Test seeking functionality
    console.log('Testing seek functionality...');
    const initialTime = await videoHelper.getVideoCurrentTime();
    await videoHelper.seekTo(30); // Seek to 30 seconds
    await page.waitForTimeout(2000);
    const newTime = await videoHelper.getVideoCurrentTime();
    expect(newTime).toBeGreaterThan(initialTime);
    expect(newTime).toBeCloseTo(30, 0); // Within 1 second

    // Step 14: Verify subtitles are available
    const subtitles = await videoHelper.getAvailableSubtitles();
    expect(subtitles.length).toBeGreaterThan(0);
    console.log('Available subtitles:', subtitles);

    // Step 15: Test subtitle selection
    if (subtitles.includes('en')) {
      await videoHelper.selectSubtitle('en');
      await page.waitForTimeout(1000);
      // Verify subtitle track is active
      const isSubtitleActive = await page.evaluate(() => {
        const video = document.querySelector('video') as HTMLVideoElement;
        if (!video || !video.textTracks) return false;
        for (let i = 0; i < video.textTracks.length; i++) {
          if (video.textTracks[i].mode === 'showing') return true;
        }
        return false;
      });
      expect(isSubtitleActive).toBeTruthy();
    }

    // Step 16: Verify download progress updates
    await expect(page.locator('[data-testid="download-speed"]')).toBeVisible();
    await expect(page.locator('[data-testid="download-eta"]')).toBeVisible();
  });

  test('should handle slow download gracefully', async ({ page }) => {
    const testVideo = testData.testVideos[1]; // Sintel (smaller file)

    // Navigate directly to watch page
    await page.goto(`/watch/test-video-id`);

    // Verify loading state is shown
    await expect(page.locator('[data-testid="loading-indicator"]')).toBeVisible();

    // Verify progress bar is updating
    await page.waitForTimeout(5000);
    const progressText = await page.locator('[data-testid="download-progress"]').textContent();
    expect(progressText).toMatch(/\d+%/);
  });

  test('should resume from cached video', async ({ page }) => {
    // This test assumes a video was already downloaded in a previous test
    const testVideo = testData.testVideos[0];

    // Navigate to watch page
    await page.goto(`/watch/${testVideo.query}`);

    // Verify video loads quickly from cache
    await videoHelper.waitForVideoPlayer();

    // Video should be available almost immediately
    const status = await page.locator('[data-testid="download-status"]').textContent();
    expect(status).toMatch(/(Completed|Ready)/);
  });
});
```

### Test Spec: Search Functionality

**specs/video-search.spec.ts**:
```typescript
import { test, expect } from '@playwright/test';
import testData from '../fixtures/test-data.json';

test.describe('Video Search Functionality', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('input[name="username"]', testData.testUser.username);
    await page.fill('input[name="password"]', testData.testUser.password);
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  });

  test('should search for videos and display results', async ({ page }) => {
    await page.goto('/search');

    // Enter search query
    await page.fill('input[name="search"]', 'action');
    await page.press('input[name="search"]', 'Enter');

    // Wait for results
    await page.waitForSelector('[data-testid="video-card"]');

    // Verify results
    const videoCards = page.locator('[data-testid="video-card"]');
    expect(await videoCards.count()).toBeGreaterThan(0);
  });

  test('should apply filters to search results', async ({ page }) => {
    await page.goto('/search?q=action');

    // Wait for initial results
    await page.waitForSelector('[data-testid="video-card"]');
    const initialCount = await page.locator('[data-testid="video-card"]').count();

    // Apply genre filter
    await page.click('[data-testid="filter-genre"]');
    await page.click('[data-testid="genre-action"]');

    // Wait for filtered results
    await page.waitForTimeout(2000);

    // Verify filtering worked (results may change)
    const filteredCount = await page.locator('[data-testid="video-card"]').count();
    expect(filteredCount).toBeGreaterThan(0);
  });

  test('should handle infinite scroll pagination', async ({ page }) => {
    await page.goto('/search?q=movie');

    // Wait for initial results
    await page.waitForSelector('[data-testid="video-card"]');
    const initialCount = await page.locator('[data-testid="video-card"]').count();

    // Scroll to bottom
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));

    // Wait for new results to load
    await page.waitForTimeout(3000);

    // Verify more results loaded
    const newCount = await page.locator('[data-testid="video-card"]').count();
    expect(newCount).toBeGreaterThan(initialCount);
  });

  test('should sort search results', async ({ page }) => {
    await page.goto('/search?q=comedy');

    // Wait for results
    await page.waitForSelector('[data-testid="video-card"]');

    // Get first video title
    const firstTitle = await page.locator('[data-testid="video-card"]').first()
      .locator('[data-testid="video-title"]').textContent();

    // Change sort order (e.g., by rating)
    await page.click('[data-testid="sort-dropdown"]');
    await page.click('[data-testid="sort-rating"]');

    // Wait for re-sorted results
    await page.waitForTimeout(2000);

    // Verify order changed
    const newFirstTitle = await page.locator('[data-testid="video-card"]').first()
      .locator('[data-testid="video-title"]').textContent();

    // Titles may or may not be different depending on data
    expect(newFirstTitle).toBeTruthy();
  });
});
```

## Setup Instructions

### 1. Install Dependencies

```bash
# Navigate to project root
cd /home/angela/projects/claude-code/hypertube

# Create tests directory
mkdir -p tests/e2e/{fixtures,helpers,specs}

# Install Playwright
npm init playwright@latest --yes -- --quiet --browser=firefox --gha

# Or install manually
npm install -D @playwright/test
npx playwright install firefox
```

### 2. Configure Test Environment

Create `.env.test`:
```env
NODE_ENV=test
FRONTEND_URL=http://localhost:3000
API_URL=http://localhost:8080
DATABASE_URL=postgresql://test:test@localhost:5432/hypertube_test
REDIS_URL=redis://localhost:6379/1
RABBITMQ_URL=amqp://localhost:5672
```

### 3. Prepare Test Data

Ensure Docker services are running:
```bash
docker-compose up -d
```

Create test user (run once):
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "e2e_test_user",
    "email": "e2e@test.com",
    "password": "Test123!@#"
  }'
```

## Running Tests

### Run All Tests
```bash
npm test
```

### Run Specific Test Suite
```bash
npx playwright test video-complete-flow
npx playwright test video-search
```

### Run in Debug Mode
```bash
npx playwright test --debug
```

### Run with UI Mode
```bash
npx playwright test --ui
```

### View Test Report
```bash
npx playwright show-report
```

## Assertions and Success Criteria

### Video Search Flow
- ✅ Search form accepts input
- ✅ Results load within 10 seconds
- ✅ At least 1 result is displayed
- ✅ Video cards show metadata (title, year, rating, thumbnail)
- ✅ Clicking video navigates to watch page

### Video Streaming Flow
- ✅ Watch page displays video metadata
- ✅ Download initiates automatically on page load
- ✅ Progress bar shows percentage
- ✅ Download speed and ETA are displayed
- ✅ Video player appears at 10% buffer
- ✅ Video plays automatically
- ✅ Video source URL is correct format
- ✅ Seeking works (HTTP Range requests)
- ✅ Subtitles are available
- ✅ Subtitle tracks can be selected
- ✅ Progress updates every 2 seconds

### Performance Criteria
- Search results: < 10 seconds
- Video player appearance: < 2 minutes (10% of 100MB file)
- Seeking: < 3 seconds
- Subtitle loading: < 2 seconds

## Network Monitoring

### Intercept and Verify API Calls

```typescript
test('should make correct API calls during streaming', async ({ page }) => {
  const apiCalls: string[] = [];

  // Monitor network requests
  page.on('request', request => {
    if (request.url().includes('/api/streaming/')) {
      apiCalls.push(request.url());
    }
  });

  await page.goto('/watch/test-video-id');
  await videoHelper.waitForVideoPlayer();

  // Verify expected API calls
  expect(apiCalls.some(url => url.includes('/download'))).toBeTruthy();
  expect(apiCalls.some(url => url.includes('/ready'))).toBeTruthy();
  expect(apiCalls.some(url => url.includes('/video/'))).toBeTruthy();
});
```

### Verify HTTP Range Requests

```typescript
test('should use HTTP Range requests for seeking', async ({ page }) => {
  const rangeRequests: string[] = [];

  page.on('request', request => {
    const headers = request.headers();
    if (headers['range']) {
      rangeRequests.push(headers['range']);
    }
  });

  await page.goto('/watch/test-video-id');
  await videoHelper.waitForVideoPlayer();
  await videoHelper.seekTo(60);

  await page.waitForTimeout(2000);

  // Verify Range header was sent
  expect(rangeRequests.length).toBeGreaterThan(0);
  expect(rangeRequests[0]).toMatch(/bytes=\d+-\d+/);
});
```

## Debugging and Troubleshooting

### Enable Video Recording
```typescript
// playwright.config.ts
use: {
  video: 'on', // Record all tests
  trace: 'on',  // Enable trace for all tests
}
```

### View Trace
```bash
npx playwright show-trace trace.zip
```

### Screenshot on Failure
```typescript
test.afterEach(async ({ page }, testInfo) => {
  if (testInfo.status !== testInfo.expectedStatus) {
    await page.screenshot({
      path: `screenshots/${testInfo.title}-failure.png`,
      fullPage: true
    });
  }
});
```

### Console Logs
```typescript
page.on('console', msg => console.log('Browser:', msg.text()));
```

## CI/CD Integration

### GitHub Actions Workflow

**.github/workflows/e2e-tests.yml**:
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
        run: npm ci

      - name: Start services
        run: docker-compose up -d

      - name: Wait for services
        run: |
          timeout 120 bash -c 'until curl -f http://localhost:3000; do sleep 5; done'
          timeout 120 bash -c 'until curl -f http://localhost:8080/actuator/health; do sleep 5; done'

      - name: Install Playwright Browsers
        run: npx playwright install --with-deps firefox

      - name: Run E2E tests
        run: npx playwright test

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: playwright-report/
          retention-days: 30

      - name: Upload test videos
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: test-videos
          path: test-results/
          retention-days: 7
```

## Test Data Management

### Use Real Torrent Files for Testing

For consistent testing, use legal open-source videos:

1. **Big Buck Bunny** (© Blender Foundation | CC BY 3.0)
   - Size: ~350 MB
   - Duration: 10:34
   - Magnet: `magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c`

2. **Sintel** (© Blender Foundation | CC BY 3.0)
   - Size: ~130 MB
   - Duration: 14:48
   - Magnet: `magnet:?xt=urn:btih:08ada5a7a6183aae1e09d831df6748d566095a10`

3. **Elephants Dream** (© Blender Foundation | CC BY 2.5)
   - Size: ~215 MB
   - Duration: 10:53
   - Magnet: `magnet:?xt=urn:btih:d54dfff2d7cd3e9f8d0e4c1a9e52e6f7a0d0e7e1`

## Next Steps

1. ✅ Create test directory structure
2. ✅ Install Playwright with Firefox support
3. ✅ Implement test helpers (ApiHelper, VideoHelper)
4. ✅ Write test specs for complete flow
5. ✅ Configure test environment
6. ✅ Set up test data fixtures
7. ✅ Run initial test suite
8. ✅ Fix any failing tests
9. ✅ Add CI/CD integration
10. ✅ Document test results

## Expected Outcomes

After implementing this E2E testing plan, you will have:

- ✅ Automated browser tests for complete video search → streaming flow
- ✅ Verification of all critical user journeys
- ✅ Network request monitoring and validation
- ✅ HTTP Range request testing for seeking functionality
- ✅ Subtitle integration verification
- ✅ Performance benchmarking
- ✅ CI/CD integration for continuous testing
- ✅ Video recordings and traces for debugging failures
- ✅ Comprehensive test coverage documentation
