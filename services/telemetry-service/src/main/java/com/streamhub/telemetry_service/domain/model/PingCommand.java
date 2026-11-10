package com.streamhub.telemetry_service.domain.model;

import java.time.Instant;

/** Playback ping accepted by POST /api/v1/telemetry/ping (spec.md 3.5). */
public record PingCommand(
        String eventId,
        String contentId,
        String episodeId,
        int progressSeconds,
        Instant timestamp
) {
}
