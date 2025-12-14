import { test } from '@playwright/test';
import testData from '../fixtures/test-data.json';

test('simple login with console logging', async ({ page }) => {
  // Capture all console messages
  page.on('console', msg => {
    console.log(`[BROWSER ${msg.type()}]:`, msg.text());
  });

  // Capture network requests
  page.on('request', request => {
    if (request.url().includes('/api/')) {
      console.log(`[REQUEST]: ${request.method()} ${request.url()}`);
    }
  });

  page.on('response', response => {
    if (response.url().includes('/api/')) {
      console.log(`[RESPONSE]: ${response.status()} ${response.url()}`);
    }
  });

  // Capture errors
  page.on('pageerror', error => {
    console.log(`[PAGE ERROR]:`, error.message);
  });

  console.log('=== Navigating to login page ===');
  await page.goto('/login');
  await page.waitForLoadState('networkidle');

  console.log('=== Filling form ===');
  await page.fill('#usernameOrEmail', testData.testUser.username);
  await page.fill('#password', testData.testUser.password);

  console.log('=== Clicking submit ===');
  await page.click('button[type="submit"]');

  console.log('=== Waiting 10s to observe behavior ===');
  await page.waitForTimeout(10000);

  console.log(`=== Final URL: ${page.url()} ===`);

  // Take screenshot for inspection
  await page.screenshot({ path: 'test-results/simple-login-final.png', fullPage: true });
});
