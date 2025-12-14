import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for HyperTube E2E tests
 * See https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './e2e/specs',

  /* Maximum time one test can run */
  timeout: 120000, // 2 minutes (video downloads can be slow)

  /* Expect timeout for assertions */
  expect: {
    timeout: 30000 // 30 seconds
  },

  /* Run tests sequentially to avoid overwhelming services */
  fullyParallel: false,

  /* Fail the build on CI if you accidentally left test.only in the source code */
  forbidOnly: !!process.env.CI,

  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,

  /* Single worker to avoid race conditions */
  workers: 1,

  /* Reporter to use */
  reporter: [
    ['html', { outputFolder: 'test-results/html-report' }],
    ['list'],
    ['junit', { outputFile: 'test-results/junit.xml' }]
  ],

  /* Shared settings for all projects */
  use: {
    /* Base URL for page.goto('/') */
    baseURL: process.env.FRONTEND_URL || 'http://localhost:3000',

    /* Collect trace on first retry */
    trace: 'on-first-retry',

    /* Record video on failure */
    video: 'retain-on-failure',

    /* Take screenshot on failure */
    screenshot: 'only-on-failure',

    /* Maximum time each action can take */
    actionTimeout: 15000,

    /* Viewport size */
    viewport: { width: 1920, height: 1080 },
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
        // Firefox-specific options
        launchOptions: {
          firefoxUserPrefs: {
            // Allow autoplay
            'media.autoplay.default': 0,
            'media.autoplay.enabled': true,
            // Enable subtitles
            'media.webvtt.enabled': true,
            // Disable download prompts
            'browser.download.folderList': 2,
            'browser.helperApps.neverAsk.saveToDisk': 'video/mp4,application/octet-stream'
          }
        }
      },
    },
  ],

  /* Start services before running tests (skip if SKIP_WEBSERVER is set) */
  ...(process.env.SKIP_WEBSERVER ? {} : {
    webServer: {
      command: 'docker-compose up',
      url: 'http://localhost:3000',
      timeout: 120000,
      reuseExistingServer: !process.env.CI,
      stdout: 'pipe',
      stderr: 'pipe',
    }
  }),
});
