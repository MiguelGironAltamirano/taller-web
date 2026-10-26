import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end tests run against a real stack (Docker Compose with VITE_USE_MOCKS=false and db/seed-dev),
 * never against the mocked API. Start one with `SMOKE_KEEP_STACK=1 bash infra/smoke.sh` or
 * `docker compose up`, then run `pnpm e2e`. E2E_BASE_URL points at the nginx entry point.
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
