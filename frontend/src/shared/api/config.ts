export const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api/v1"
).replace(/\/+$/, "");

export const USE_MOCKS = process.env.NEXT_PUBLIC_USE_MOCKS === "true";

export type MockScope = "all" | "missing";

/**
 * `all`: every endpoint is mocked (offline demo and E2E).
 * `missing`: only the endpoints whose services do not exist yet are mocked
 * (telemetry and interactions); auth and home go to Kong.
 */
export const MOCK_SCOPE: MockScope =
  process.env.NEXT_PUBLIC_MOCK_SCOPE === "missing" ? "missing" : "all";
