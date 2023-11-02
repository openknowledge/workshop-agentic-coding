import { defineConfig, devices } from '@playwright/test';

const FRONTEND_URL = 'http://localhost:5173';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  reporter: 'html',
  globalSetup: './e2e/global-setup',
  globalTeardown: './e2e/global-teardown',
  use: {
    baseURL: FRONTEND_URL,
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: FRONTEND_URL,
    timeout: 120_000,
    reuseExistingServer: !process.env.CI && !process.env.PLAYWRIGHT_COVERAGE,
    env: process.env.PLAYWRIGHT_COVERAGE ? { VITE_COVERAGE: 'true' } : {},
  },
});
