import { test } from '@playwright/test';

test('debug login with console', async ({ page }) => {
  // Capture console messages
  page.on('console', msg => console.log('BROWSER:', msg.type(), msg.text()));
  page.on('pageerror', error => console.log('PAGE ERROR:', error.message));
  
  console.log('=== Navigating to login ===');
  await page.goto('http://localhost:3000/login');
  
  console.log('=== Filling form ===');
  await page.fill('#usernameOrEmail', 'e2e_user');
  await page.fill('#password', 'TestPass123');
  
  console.log('=== Clicking submit ===');
  await page.click('button[type="submit"]');
  
  console.log('=== Waiting 10s ===');
  await page.waitForTimeout(10000);
  
  console.log('=== Final URL:', page.url());
});
