package com.streamhub.telemetry_service.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamhub.telemetry_service.domain.model.Rating;
import com.streamhub.telemetry_service.domain.model.RatingCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/** Request body of spec.md 3.6. */
public final class InteractionDtos {

    private static final String UUID_V4 =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private InteractionDtos() {
    }

    public record RatingRequest(
            @NotBlank
            @Pattern(regexp = UUID_V4, message = "must be a UUID v4")
            @JsonProperty("event_id") String eventId,
            @NotBlank @JsonProperty("content_id") String contentId,
            @NotNull Rating rating,
            @NotNull Instant timestamp
    ) {

        public RatingCommand toCommand() {
            return new RatingCommand(eventId, contentId, rating, timestamp);
        }
    }
}
