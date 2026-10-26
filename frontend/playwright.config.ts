import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end tests run against a real stack (Docker Compose with VITE_USE_MOCKS=false, db/seed-dev and db/seed-demo),
 * never against the mocked API. `bash infra/e2e.sh` builds that stack, runs this suite and always tears it down.
 * Against a stack that is already running, use `E2E_BASE_URL=http://localhost:<port> pnpm e2e` (the nginx entry point).
 * Every test creates the data it changes, so the suite can run again on the same stack.
 */
export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8089',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
