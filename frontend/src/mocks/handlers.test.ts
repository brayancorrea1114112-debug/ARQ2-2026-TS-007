import { describe, expect, it } from "vitest";
import { allHandlers, authHandlers, missingHandlers } from "@/mocks/handlers";

describe("mock scopes", () => {
  it("keeps auth and profiles out of the missing-services scope", () => {
    expect(authHandlers).toHaveLength(3);
    expect(missingHandlers).toHaveLength(2);
    expect(allHandlers).toHaveLength(6);
  });
});
