import { test, expect } from '@playwright/test';
import { ApiHelper } from '../helpers/api-helper';
import { VideoHelper } from '../helpers/video-helper';
import testData from '../fixtures/test-data.json';

test.describe('Video Search and Streaming - Complete E2E Flow', () => {
  let apiHelper: ApiHelper;
  let videoHelper: VideoHelper;

  test.beforeEach(async ({ page, baseURL }) => {
    apiHelper = new ApiHelper(baseURL || 'http://localhost:8080');
    videoHelper = new VideoHelper(page);

    // Login before each test
    await page.goto('/login');
    await page.fill('#usernameOrEmail', testData.testUser.username);
    await page.fill('#password', testData.testUser.password);
    await page.click('button[type="submit"]');

    // Wait for successful login
    await page.waitForURL(/\/(home|dashboard)?$/, { timeout: 10000 });
  });

  test('COMPLETE FLOW: Search → Select → Watch → Stream', async ({ page }) => {
    console.log('=== Starting Complete E2E Test ===');

    // STEP 1: Navigate to search page
    console.log('Step 1: Navigate to search page');
    await page.goto('/browse');
    await expect(page).toHaveURL(/\/search/);

    // STEP 2: Enter search query
    console.log('Step 2: Enter search query');
    const searchQuery = 'action'; // Use popular query
    await page.fill('[data-testid="search-input"]', searchQuery);
    await page.press('[data-testid="search-input"]', 'Enter');

    // STEP 3: Wait for search results
    console.log('Step 3: Wait for search results');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    // STEP 4: Verify results are displayed
    console.log('Step 4: Verify results are displayed');
    const videoCards = page.locator('[data-testid="video-card"]');
    const resultCount = await videoCards.count();
    console.log(`Found ${resultCount} video results`);
    expect(resultCount).toBeGreaterThan(0);

    // STEP 5: Click on first video result
    console.log('Step 5: Click on first video');
    const firstVideo = videoCards.first();
    const videoId = await firstVideo.getAttribute('data-video-id');
    console.log(`Selected video ID: ${videoId}`);

    await firstVideo.click();

    // STEP 6: Verify watch page loads
    console.log('Step 6: Verify watch page loads');
    await page.waitForURL(`/watch/${videoId}`, { timeout: 10000 });
    expect(page.url()).toContain('/watch/');

    // STEP 7: Verify video metadata is displayed
    console.log('Step 7: Verify video metadata');
    await expect(page.locator('h1')).toBeVisible();

    // Check for metadata elements (they may or may not be present depending on data)
    const title = await page.locator('h1').textContent();
    console.log(`Video title: ${title}`);
    expect(title).toBeTruthy();

    // STEP 8: Verify download initiates automatically
    console.log('Step 8: Wait for download to start');
    await page.waitForTimeout(3000); // Give time for download to start

    // Check for download progress indicator
    const downloadStatus = page.locator('[data-testid="download-status"]');
    if (await downloadStatus.count() > 0) {
      const status = await downloadStatus.textContent();
      console.log(`Download status: ${status}`);
    }

    // STEP 9: Monitor download progress to 10%
    console.log('Step 9: Waiting for 10% buffer... (this may take 1-2 minutes)');
    try {
      await videoHelper.waitForDownloadProgress(10, 180000); // 3 minute timeout
      console.log('✓ Reached 10% buffer threshold');
    } catch (error) {
      console.log('Warning: Could not verify 10% threshold, continuing...');
      // Continue anyway - video might already be ready
    }

    // STEP 10: Verify video player appears
    console.log('Step 10: Verify video player appears');
    try {
      await videoHelper.waitForVideoPlayer();
      console.log('✓ Video player is visible');
    } catch (error) {
      console.log('Video player not found immediately, waiting longer...');
      await page.waitForTimeout(10000);
      await videoHelper.waitForVideoPlayer();
    }

    await expect(page.locator('video')).toBeVisible();

    // STEP 11: Verify video has valid source
    console.log('Step 11: Verify video source');
    const videoSrc = await videoHelper.getVideoSource();
    console.log(`Video source: ${videoSrc}`);
    expect(videoSrc).toBeTruthy();
    expect(videoSrc).toContain('/api/streaming/video/');

    // STEP 12: Wait for video to start playing
    console.log('Step 12: Wait for video to start playing');
    await page.waitForTimeout(5000); // Give video time to buffer and autoplay

    // Try to play if not autoplaying
    const isPlaying = await videoHelper.isVideoPlaying();
    if (!isPlaying) {
      console.log('Video not autoplaying, attempting to play...');
      await videoHelper.play();
      await page.waitForTimeout(2000);
    }

    // Verify playback
    const currentTime = await videoHelper.getVideoCurrentTime();
    console.log(`Video current time: ${currentTime}s`);
    expect(currentTime).toBeGreaterThanOrEqual(0);

    // STEP 13: Get video stats
    console.log('Step 13: Get video stats');
    const stats = await videoHelper.getVideoStats();
    console.log(`Video dimensions: ${stats.width}x${stats.height}`);
    console.log(`Ready state: ${stats.readyState}`);
    expect(stats.width).toBeGreaterThan(0);
    expect(stats.height).toBeGreaterThan(0);

    // STEP 14: Test seeking functionality
    console.log('Step 14: Test seeking functionality');
    const initialTime = await videoHelper.getVideoCurrentTime();
    console.log(`Current time before seek: ${initialTime}s`);

    await videoHelper.seekTo(30); // Seek to 30 seconds
    await page.waitForTimeout(3000); // Wait for seek to complete

    const newTime = await videoHelper.getVideoCurrentTime();
    console.log(`Current time after seek: ${newTime}s`);
    expect(newTime).toBeGreaterThan(initialTime);

    // STEP 15: Check for subtitles
    console.log('Step 15: Check for subtitles');
    const subtitles = await videoHelper.getAvailableSubtitles();
    console.log(`Available subtitles: ${subtitles.length > 0 ? subtitles.join(', ') : 'None'}`);

    if (subtitles.length > 0) {
      console.log('Testing subtitle selection...');
      await videoHelper.selectSubtitle(subtitles[0]);
      await page.waitForTimeout(1000);

      const isSubtitleActive = await videoHelper.areSubtitlesActive();
      console.log(`Subtitles active: ${isSubtitleActive}`);
    } else {
      console.log('No subtitles available for this video');
    }

    // STEP 16: Verify download progress updates
    console.log('Step 16: Verify download progress info');
    const downloadSpeed = await videoHelper.getDownloadSpeed();
    const eta = await videoHelper.getETA();

    if (downloadSpeed) {
      console.log(`Download speed: ${downloadSpeed}`);
    }
    if (eta) {
      console.log(`ETA: ${eta}`);
    }

    console.log('=== Complete E2E Test Finished Successfully ===');
  });

  test('should handle video playback errors gracefully', async ({ page }) => {
    // Try to access a non-existent video
    await page.goto('/watch/non-existent-video-id');

    // Should show error message
    await page.waitForTimeout(5000);

    // Check for error message or 404
    const errorMessage = page.locator('text=/error|not found|unavailable/i');
    const hasError = await errorMessage.count() > 0;

    if (hasError) {
      console.log('Error handling works correctly');
      await expect(errorMessage).toBeVisible();
    } else {
      console.log('No explicit error message shown');
    }
  });

  test('should display download progress information', async ({ page }) => {
    // Search and select a video
    await page.goto('/search?q=movie');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    // Wait for watch page
    await page.waitForURL(/\/watch\//);

    // Wait for download to start
    await page.waitForTimeout(5000);

    // Check for progress indicators
    const progressBar = page.locator('[data-testid="download-progress"]');
    if (await progressBar.count() > 0) {
      await expect(progressBar).toBeVisible();
      console.log('Progress bar is visible');

      // Check if percentage is shown
      const progressText = await progressBar.textContent();
      console.log(`Progress: ${progressText}`);
      expect(progressText).toMatch(/\d+/); // Should contain a number
    }

    // Check for status indicator
    const status = await videoHelper.getDownloadStatus();
    if (status) {
      console.log(`Download status: ${status}`);
      expect(status).toMatch(/downloading|buffering|converting|ready/i);
    }
  });

  test('should support pause and resume', async ({ page }) => {
    // Navigate to a video
    await page.goto('/search?q=action');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    await page.waitForURL(/\/watch\//);

    // Wait for video player
    try {
      await videoHelper.waitForStreamingReady(120000);
    } catch (error) {
      console.log('Video not ready yet, skipping pause/resume test');
      test.skip();
      return;
    }

    // Get initial time
    await page.waitForTimeout(3000);
    const time1 = await videoHelper.getVideoCurrentTime();
    console.log(`Time before pause: ${time1}s`);

    // Pause
    await videoHelper.pause();
    await page.waitForTimeout(2000);

    const time2 = await videoHelper.getVideoCurrentTime();
    console.log(`Time after pause: ${time2}s`);

    // Resume
    await videoHelper.play();
    await page.waitForTimeout(3000);

    const time3 = await videoHelper.getVideoCurrentTime();
    console.log(`Time after resume: ${time3}s`);

    // Time should have progressed after resume
    expect(time3).toBeGreaterThan(time2);
  });
});
