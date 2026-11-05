import { apiRequest } from "@/shared/api/client";
import type { AcceptedResponse, Rating } from "@/shared/api/contracts";
import { createEventId } from "@/shared/lib/ids";

export function sendRating(
  token: string,
  contentId: string,
  rating: Rating,
): Promise<AcceptedResponse> {
  return apiRequest<AcceptedResponse>("/interactions/like", {
    method: "POST",
    token,
    body: {
      event_id: createEventId(),
      content_id: contentId,
      rating,
      timestamp: new Date().toISOString(),
    },
  });
}
