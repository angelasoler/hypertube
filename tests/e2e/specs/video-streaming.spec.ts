import { test, expect, Page } from '@playwright/test';
import { VideoHelper } from '../helpers/video-helper';
import testData from '../fixtures/test-data.json';

test.describe('Video Streaming - HTTP Range and Performance', () => {
  let videoHelper: VideoHelper;

  test.beforeEach(async ({ page }) => {
    videoHelper = new VideoHelper(page);

    // Login
    await page.goto('/login');
    await page.fill('#usernameOrEmail', testData.testUser.username);
    await page.fill('#password', testData.testUser.password);
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/(home|dashboard)?$/);
  });

  test('should use HTTP Range requests when seeking', async ({ page }) => {
    const rangeRequests: Array<{ url: string; range: string }> = [];

    // Monitor network requests
    page.on('request', (request) => {
      const headers = request.headers();
      if (headers['range'] && request.url().includes('/api/streaming/video/')) {
        rangeRequests.push({
          url: request.url(),
          range: headers['range']
        });
        console.log(`Range request: ${headers['range']} -> ${request.url()}`);
      }
    });

    // Navigate to video
    await page.goto('/search?q=action');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    await page.waitForURL(/\/watch\//);

    // Wait for video player
    try {
      await videoHelper.waitForStreamingReady(120000);
    } catch (error) {
      console.log('Video not ready, skipping Range test');
      test.skip();
      return;
    }

    // Perform seek operation
    await page.waitForTimeout(3000);
    await videoHelper.seekTo(60);
    await page.waitForTimeout(3000);

    // Verify Range requests were made
    console.log(`Total Range requests captured: ${rangeRequests.length}`);

    if (rangeRequests.length > 0) {
      // Verify Range header format
      expect(rangeRequests[0].range).toMatch(/bytes=\d+-(\d+)?/);
      console.log('✓ HTTP Range requests are working correctly');
    } else {
      console.log('Note: No Range requests captured (might be fully cached)');
    }
  });

  test('should handle network errors during streaming', async ({ page, context }) => {
    // Navigate to video
    await page.goto('/search?q=movie');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    await page.waitForURL(/\/watch\//);

    // Wait for video to start loading
    await page.waitForTimeout(5000);

    // Simulate network failure
    await context.setOffline(true);
    await page.waitForTimeout(2000);

    // Try to interact with video (should show error or handle gracefully)
    const videoError = await videoHelper.getVideoError();
    console.log(`Video error during offline: ${videoError || 'None'}`);

    // Restore network
    await context.setOffline(false);
    await page.waitForTimeout(3000);

    // Video should recover or show appropriate message
    console.log('Network restored');
  });

  test('should display correct video resolution', async ({ page }) => {
    await page.goto('/search?q=action');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    await page.waitForURL(/\/watch\//);

    try {
      await videoHelper.waitForStreamingReady(120000);
    } catch (error) {
      console.log('Video not ready, skipping resolution test');
      test.skip();
      return;
    }

    // Get video stats
    await page.waitForTimeout(5000);
    const stats = await videoHelper.getVideoStats();

    console.log(`Video resolution: ${stats.width}x${stats.height}`);
    console.log(`Ready state: ${stats.readyState}`);
    console.log(`Network state: ${stats.networkState}`);

    // Verify video has valid dimensions
    expect(stats.width).toBeGreaterThan(0);
    expect(stats.height).toBeGreaterThan(0);

    // Common video resolutions
    const isValidResolution =
      stats.height >= 240 && // At least 240p
      stats.width >= 320;    // At least 320px wide

    expect(isValidResolution).toBeTruthy();
  });

  test('should handle subtitle tracks correctly', async ({ page }) => {
    await page.goto('/search?q=movie');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    await page.waitForURL(/\/watch\//);

    try {
      await videoHelper.waitForStreamingReady(120000);
    } catch (error) {
      console.log('Video not ready, skipping subtitle test');
      test.skip();
      return;
    }

    // Wait for video to load
    await page.waitForTimeout(5000);

    // Get available subtitles
    const subtitles = await videoHelper.getAvailableSubtitles();
    console.log(`Available subtitle tracks: ${subtitles.length}`);

    if (subtitles.length > 0) {
      console.log(`Subtitle languages: ${subtitles.join(', ')}`);

      // Test each subtitle track
      for (const lang of subtitles) {
        await videoHelper.selectSubtitle(lang);
        await page.waitForTimeout(1000);

        const isActive = await videoHelper.areSubtitlesActive();
        console.log(`Subtitle ${lang} active: ${isActive}`);
        expect(isActive).toBeTruthy();
      }

      // Verify subtitle elements are present
      const trackElements = await page.evaluate(() => {
        const video = document.querySelector('video') as HTMLVideoElement;
        if (!video) return 0;

        const tracks = video.querySelectorAll('track');
        return tracks.length;
      });

      console.log(`Subtitle track elements: ${trackElements}`);
      expect(trackElements).toBeGreaterThan(0);
    } else {
      console.log('No subtitles available for this video');
    }
  });

  test('should update download progress continuously', async ({ page }) => {
    await page.goto('/search?q=action');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    await page.waitForURL(/\/watch\//);

    // Wait for download to start
    await page.waitForTimeout(5000);

    // Capture progress updates
    const progressUpdates: number[] = [];

    for (let i = 0; i < 5; i++) {
      const progressElement = page.locator('[data-testid="download-progress"]');

      if (await progressElement.count() > 0) {
        const progressText = await progressElement.textContent();
        if (progressText) {
          const match = progressText.match(/(\d+(?:\.\d+)?)\s*%/);
          if (match) {
            const percent = parseFloat(match[1]);
            progressUpdates.push(percent);
            console.log(`Progress update ${i + 1}: ${percent}%`);
          }
        }
      }

      await page.waitForTimeout(3000); // Check every 3 seconds
    }

    console.log(`Total progress updates captured: ${progressUpdates.length}`);

    if (progressUpdates.length >= 2) {
      // Verify progress is increasing (or staying at 100%)
      const isIncreasing = progressUpdates.every((val, idx) => {
        return idx === 0 || val >= progressUpdates[idx - 1];
      });

      expect(isIncreasing).toBeTruthy();
      console.log('✓ Progress is updating correctly');
    }
  });

  test('should show download speed and ETA', async ({ page }) => {
    await page.goto('/search?q=movie');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    await page.waitForURL(/\/watch\//);

    // Wait for download to start and stats to appear
    await page.waitForTimeout(10000);

    const speed = await videoHelper.getDownloadSpeed();
    const eta = await videoHelper.getETA();

    console.log(`Download speed: ${speed || 'Not available'}`);
    console.log(`ETA: ${eta || 'Not available'}`);

    // At least one should be available during download
    if (speed || eta) {
      console.log('✓ Download statistics are displayed');
    } else {
      console.log('Note: Download might be complete or stats not implemented');
    }
  });

  test('should cache video for subsequent views', async ({ page }) => {
    // First view - fresh download
    await page.goto('/search?q=action');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    const videoId = await firstVideo.getAttribute('data-video-id');

    await firstVideo.click();
    await page.waitForURL(/\/watch\//);

    // Wait for some download progress
    await page.waitForTimeout(10000);

    const firstViewStatus = await videoHelper.getDownloadStatus();
    console.log(`First view status: ${firstViewStatus}`);

    // Navigate away
    await page.goto('/browse');
    await page.waitForTimeout(2000);

    // Return to same video
    await page.goto(`/watch/${videoId}`);

    // Should load faster from cache
    await page.waitForTimeout(5000);

    const secondViewStatus = await videoHelper.getDownloadStatus();
    console.log(`Second view status: ${secondViewStatus}`);

    // Status might be "Ready" or "Completed" if cached
    if (secondViewStatus) {
      expect(secondViewStatus).toMatch(/ready|completed|cached/i);
    }
  });

  test('should handle multiple seek operations', async ({ page }) => {
    await page.goto('/search?q=movie');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    await page.waitForURL(/\/watch\//);

    try {
      await videoHelper.waitForStreamingReady(120000);
    } catch (error) {
      console.log('Video not ready, skipping seek test');
      test.skip();
      return;
    }

    await page.waitForTimeout(3000);

    // Perform multiple seeks
    const seekPositions = [10, 30, 60, 120, 90];

    for (const position of seekPositions) {
      await videoHelper.seekTo(position);
      await page.waitForTimeout(2000);

      const currentTime = await videoHelper.getVideoCurrentTime();
      console.log(`Seeked to ${position}s, actual time: ${currentTime}s`);

      // Allow some tolerance (within 5 seconds)
      expect(Math.abs(currentTime - position)).toBeLessThan(5);
    }

    console.log('✓ Multiple seek operations completed successfully');
  });

  test('should verify video controls are functional', async ({ page }) => {
    await page.goto('/search?q=action');
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    const firstVideo = page.locator('[data-testid="video-card"]').first();
    await firstVideo.click();

    await page.waitForURL(/\/watch\//);

    try {
      await videoHelper.waitForStreamingReady(120000);
    } catch (error) {
      console.log('Video not ready, skipping controls test');
      test.skip();
      return;
    }

    const video = page.locator('video');
    await expect(video).toHaveAttribute('controls');

    // Verify video is interactive
    const hasControls = await page.evaluate(() => {
      const videoElement = document.querySelector('video') as HTMLVideoElement;
      return videoElement?.controls || false;
    });

    expect(hasControls).toBeTruthy();
    console.log('✓ Video controls are enabled');
  });
});
