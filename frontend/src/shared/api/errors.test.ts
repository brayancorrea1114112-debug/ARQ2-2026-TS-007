import { describe, expect, it } from "vitest";
import { parseApiError } from "@/shared/api/errors";

describe("parseApiError", () => {
  it("reads the standard error envelope", () => {
    const error = parseApiError(
      403,
      {
        error: {
          code: "PROFILE_NOT_SELECTED",
          message: "Select a profile first.",
          correlation_id: "corr-1",
        },
      },
      "fallback",
    );

    expect(error.code).toBe("PROFILE_NOT_SELECTED");
    expect(error.message).toBe("Select a profile first.");
    expect(error.correlationId).toBe("corr-1");
  });

  it("falls back by status code", () => {
    const error = parseApiError(429, null, "corr-2");

    expect(error.code).toBe("RATE_LIMITED");
    expect(error.message).toContain("demasiadas solicitudes");
    expect(error.correlationId).toBe("corr-2");
  });

  it("never throws on an unexpected body", () => {
    const error = parseApiError(500, "<html>", null);

    expect(error.code).toBe("INTERNAL_ERROR");
    expect(error.status).toBe(500);
  });
});
