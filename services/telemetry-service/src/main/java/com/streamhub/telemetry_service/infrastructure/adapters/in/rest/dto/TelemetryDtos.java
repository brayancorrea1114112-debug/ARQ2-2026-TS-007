package com.streamhub.telemetry_service.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamhub.telemetry_service.domain.model.PingCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/** Request and response bodies of spec.md 3.5. */
public final class TelemetryDtos {

    private static final String UUID_V4 =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private TelemetryDtos() {
    }

    public record PingRequest(
            @NotBlank
            @Pattern(regexp = UUID_V4, message = "must be a UUID v4")
            @JsonProperty("event_id") String eventId,
            @NotBlank @JsonProperty("content_id") String contentId,
            @JsonProperty("episode_id") String episodeId,
            @NotNull @Min(0) @JsonProperty("progress_seconds") Integer progressSeconds,
            @NotNull @JsonProperty("timestamp") Instant timestamp
    ) {

        public PingCommand toCommand() {
            return new PingCommand(eventId, contentId, episodeId, progressSeconds, timestamp);
        }
    }

    public record AcceptedResponse(@JsonProperty("event_id") String eventId, String status) {

        public static AcceptedResponse accepted(String eventId) {
            return new AcceptedResponse(eventId, "ACCEPTED");
        }
    }
}
