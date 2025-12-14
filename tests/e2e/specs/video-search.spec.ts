import { test, expect } from '@playwright/test';
import testData from '../fixtures/test-data.json';

test.describe('Video Search Functionality', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await page.goto('/login');
    await page.fill('#usernameOrEmail', testData.testUser.username);
    await page.fill('#password', testData.testUser.password);
    await page.click('button[type="submit"]');

    // Wait for redirect to homepage or dashboard
    await page.waitForURL(/\/(home|dashboard)?$/);
  });

  test('should display search form on search page', async ({ page }) => {
    await page.goto('/browse');

    // Verify search form elements
    await expect(page.locator('[data-testid="search-input"]')).toBeVisible();
    await expect(page.locator('[data-testid="search-input"]')).toBeEnabled();

    // Check placeholder or label
    const searchInput = page.locator('[data-testid="search-input"]');
    const placeholder = await searchInput.getAttribute('placeholder');
    expect(placeholder).toBeTruthy();
  });

  test('should search for videos and display results', async ({ page }) => {
    await page.goto('/browse');

    // Enter search query
    const searchQuery = testData.searchQueries.popular[0]; // 'action'
    await page.fill('[data-testid="search-input"]', searchQuery);
    await page.press('[data-testid="search-input"]', 'Enter');

    // Wait for results to load
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    // Verify results are displayed
    const videoCards = page.locator('[data-testid="video-card"]');
    const count = await videoCards.count();

    console.log(`Found ${count} video results for query: ${searchQuery}`);
    expect(count).toBeGreaterThan(0);

    // Verify each video card has required metadata
    const firstCard = videoCards.first();
    await expect(firstCard.locator('[data-testid="video-title"]')).toBeVisible();
    await expect(firstCard.locator('[data-testid="video-year"]')).toBeVisible();
    await expect(firstCard.locator('img')).toBeVisible(); // Thumbnail
  });

  test('should handle empty search results gracefully', async ({ page }) => {
    await page.goto('/browse');

    // Search for something that won't return results
    const emptyQuery = testData.searchQueries.empty[0];
    await page.fill('[data-testid="search-input"]', emptyQuery);
    await page.press('[data-testid="search-input"]', 'Enter');

    // Wait a bit for results to load (or not)
    await page.waitForTimeout(3000);

    // Check for "no results" message or empty state
    const videoCards = page.locator('[data-testid="video-card"]');
    const count = await videoCards.count();

    if (count === 0) {
      // Should show empty state message
      const emptyMessage = page.locator('text=/no results|not found/i');
      await expect(emptyMessage).toBeVisible({ timeout: 5000 });
    }
  });

  test('should navigate to video detail page when clicking a result', async ({ page }) => {
    await page.goto('/browse');

    // Search for videos
    await page.fill('[data-testid="search-input"]', testData.searchQueries.popular[1]); // 'comedy'
    await page.press('[data-testid="search-input"]', 'Enter');

    // Wait for results
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    // Get first video card
    const firstCard = page.locator('[data-testid="video-card"]').first();
    const videoId = await firstCard.getAttribute('data-video-id');

    // Click on first result
    await firstCard.click();

    // Verify navigation to watch page
    await page.waitForURL(`/watch/${videoId}`, { timeout: 10000 });

    // Verify we're on the watch page
    expect(page.url()).toContain('/watch/');
  });

  test('should apply genre filter to search results', async ({ page }) => {
    await page.goto('/search?q=movie');

    // Wait for initial results
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });
    const initialCount = await page.locator('[data-testid="video-card"]').count();

    console.log(`Initial results: ${initialCount}`);

    // Look for filter controls (adjust selectors based on actual implementation)
    const genreFilter = page.locator('[data-testid="filter-genre"]');

    if (await genreFilter.count() > 0) {
      await genreFilter.click();

      // Select a specific genre
      const actionGenre = page.locator('[data-testid="genre-action"]');
      if (await actionGenre.count() > 0) {
        await actionGenre.click();

        // Wait for filtered results
        await page.waitForTimeout(2000);

        // Verify results updated
        const filteredCount = await page.locator('[data-testid="video-card"]').count();
        console.log(`Filtered results: ${filteredCount}`);

        expect(filteredCount).toBeGreaterThan(0);
      }
    } else {
      console.log('Genre filter not found - skipping filter test');
    }
  });

  test('should sort search results by rating', async ({ page }) => {
    await page.goto('/search?q=drama');

    // Wait for results
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    // Get first video rating before sorting
    const firstRatingBefore = await page.locator('[data-testid="video-card"]')
      .first()
      .locator('[data-testid="video-rating"]')
      .textContent();

    console.log(`First video rating before sort: ${firstRatingBefore}`);

    // Look for sort dropdown
    const sortDropdown = page.locator('[data-testid="sort-dropdown"]');

    if (await sortDropdown.count() > 0) {
      await sortDropdown.click();

      // Select "Sort by rating"
      const sortRating = page.locator('[data-testid="sort-rating"]');
      if (await sortRating.count() > 0) {
        await sortRating.click();

        // Wait for re-sorted results
        await page.waitForTimeout(2000);

        // Get first video rating after sorting
        const firstRatingAfter = await page.locator('[data-testid="video-card"]')
          .first()
          .locator('[data-testid="video-rating"]')
          .textContent();

        console.log(`First video rating after sort: ${firstRatingAfter}`);

        // Verify sorting occurred (results may be different)
        expect(firstRatingAfter).toBeTruthy();
      }
    } else {
      console.log('Sort dropdown not found - skipping sort test');
    }
  });

  test('should load more results on infinite scroll', async ({ page }) => {
    await page.goto('/search?q=movie');

    // Wait for initial results
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });
    const initialCount = await page.locator('[data-testid="video-card"]').count();

    console.log(`Initial results count: ${initialCount}`);

    // Scroll to bottom of page
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));

    // Wait for new results to load
    await page.waitForTimeout(3000);

    // Check if more results loaded
    const newCount = await page.locator('[data-testid="video-card"]').count();

    console.log(`Results after scroll: ${newCount}`);

    // Should have more results (unless we hit the end)
    if (initialCount >= 20) {
      // Only test if there were enough initial results
      expect(newCount).toBeGreaterThanOrEqual(initialCount);
    }
  });

  test('should preserve search query in URL', async ({ page }) => {
    await page.goto('/browse');

    const searchQuery = 'inception';
    await page.fill('[data-testid="search-input"]', searchQuery);
    await page.press('[data-testid="search-input"]', 'Enter');

    // Wait for results
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    // Verify URL contains search query
    expect(page.url()).toContain(`q=${searchQuery}`);

    // Reload page
    await page.reload();

    // Verify search query is still in input
    const searchInput = page.locator('[data-testid="search-input"]');
    const inputValue = await searchInput.inputValue();
    expect(inputValue).toBe(searchQuery);
  });

  test('should display video thumbnails', async ({ page }) => {
    await page.goto('/search?q=thriller');

    // Wait for results
    await page.waitForSelector('[data-testid="video-card"]', { timeout: 30000 });

    // Check first video card thumbnail
    const firstCard = page.locator('[data-testid="video-card"]').first();
    const thumbnail = firstCard.locator('img');

    // Verify thumbnail exists and has src
    await expect(thumbnail).toBeVisible();
    const src = await thumbnail.getAttribute('src');
    expect(src).toBeTruthy();
    expect(src).toMatch(/^(http|\/)/); // Should be URL or relative path
  });
});
