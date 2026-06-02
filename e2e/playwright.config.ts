import { defineConfig, devices } from '@playwright/test';

const headless = process.env.PW_HEADLESS !== 'false';
const isCI = !!process.env.CI;

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  workers: 1,
  retries: isCI ? 1 : 0,
  reporter: 'list',
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
    headless,
  },
  projects: [
    {
      name: 'pwa',
      testIgnore: [/backoffice/i, /pwa-mobile/i],
      use: {
        ...devices['Desktop Chrome'],
        baseURL: process.env.PWA_BASE_URL ?? 'http://localhost:4200',
        viewport: { width: 1366, height: 900 },
      },
    },
    {
      name: 'pwa-mobile',
      testMatch: /.*pwa-mobile.*\.spec\.ts/,
      use: {
        ...devices['Pixel 7'],
        baseURL: process.env.PWA_BASE_URL ?? 'http://localhost:4200',
      },
    },
    {
      name: 'backoffice',
      testMatch: /.*backoffice.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        baseURL: process.env.BACKOFFICE_BASE_URL ?? 'http://localhost:4201',
        viewport: { width: 1440, height: 960 },
      },
    },
  ],
});
