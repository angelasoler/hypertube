import { test } from '@playwright/test';
import testData from '../fixtures/test-data.json';

test('debug browse page navigation', async ({ page }) => {
  // Capture all console messages
  page.on('console', msg => {
    console.log(`[BROWSER ${msg.type()}]:`, msg.text());
  });

  // Capture network requests
  page.on('request', request => {
    console.log(`[REQUEST]: ${request.method()} ${request.url()}`);
  });

  page.on('response', async response => {
    const url = response.url();
    console.log(`[RESPONSE]: ${response.status()} ${url}`);

    // Log API response bodies
    if (url.includes('/api/')) {
      try {
        const body = await response.text();
        console.log(`[RESPONSE BODY]: ${body.substring(0, 200)}`);
      } catch (e) {
        console.log(`[RESPONSE BODY]: Unable to read`);
      }
    }
  });

  // Capture errors
  page.on('pageerror', error => {
    console.log(`[PAGE ERROR]:`, error.message);
  });

  console.log('=== Step 1: Navigate to login page ===');
  await page.goto('/login');
  await page.waitForLoadState('networkidle');

  console.log('=== Step 2: Fill login form ===');
  await page.fill('#usernameOrEmail', testData.testUser.username);
  await page.fill('#password', testData.testUser.password);

  console.log('=== Step 3: Submit login ===');
  await page.click('button[type="submit"]');

  console.log('=== Step 4: Wait for redirect after login ===');
  await page.waitForURL(/\/(home|dashboard)?$/);
  console.log(`After login URL: ${page.url()}`);

  console.log('=== Step 5: Navigate to /browse ===');
  await page.goto('/browse');

  console.log('=== Step 6: Wait for page load ===');
  await page.waitForLoadState('networkidle');
  console.log(`Browse page URL: ${page.url()}`);

  console.log('=== Step 7: Wait 5 seconds to observe ===');
  await page.waitForTimeout(5000);

  console.log(`=== Final URL: ${page.url()} ===`);

  // Take screenshot
  await page.screenshot({ path: 'test-results/debug-browse-final.png', fullPage: true });
});
