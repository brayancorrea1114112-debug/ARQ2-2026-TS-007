import { setupWorker } from "msw/browser";
import { MOCK_SCOPE } from "@/shared/api/config";
import { allHandlers, missingHandlers } from "@/mocks/handlers";

const handlers = MOCK_SCOPE === "missing" ? missingHandlers : allHandlers;

export const worker = setupWorker(...handlers);

let startPromise: Promise<void> | null = null;

export function startMockWorker(): Promise<void> {
  if (!startPromise) {
    startPromise = worker
      .start({
        onUnhandledFrame: "bypass",
        quiet: true,
        serviceWorker: { url: "/mockServiceWorker.js" },
      })
      .then(() => undefined);
  }
  return startPromise;
}
