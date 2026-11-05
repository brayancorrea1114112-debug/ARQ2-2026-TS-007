import { defineConfig, devices } from "@playwright/test";

const realBackend = process.env.E2E_REAL === "1";
const port = realBackend ? 3001 : 3101;

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: "list",
  use: {
    baseURL: `http://localhost:${port}`,
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: {
    command: realBackend ? "npm run dev" : "npm run dev:e2e",
    url: `http://localhost:${port}`,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    env: realBackend
      ? {
          NEXT_PUBLIC_USE_MOCKS: "true",
          NEXT_PUBLIC_MOCK_SCOPE: "missing",
        }
      : {
          NEXT_PUBLIC_USE_MOCKS: "true",
          NEXT_PUBLIC_MOCK_SCOPE: "all",
        },
  },
});
