import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  retries: 1,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        headless: false,
        viewport: { width: 1366, height: 900 },
      },
    },
  ],
});
