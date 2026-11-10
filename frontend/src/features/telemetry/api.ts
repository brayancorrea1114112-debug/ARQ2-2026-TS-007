import { apiRequest } from "@/shared/api/client";
import type { AcceptedResponse, PingRequest } from "@/shared/api/contracts";

export function sendPing(
  token: string,
  payload: PingRequest,
  keepalive = false,
): Promise<AcceptedResponse> {
  return apiRequest<AcceptedResponse>("/telemetry/ping", {
    method: "POST",
    token,
    body: payload,
    keepalive,
  });
}
