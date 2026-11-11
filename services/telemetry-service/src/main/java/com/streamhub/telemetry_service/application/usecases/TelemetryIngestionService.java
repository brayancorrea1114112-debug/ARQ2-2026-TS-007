package com.streamhub.telemetry_service.application.usecases;

import com.streamhub.telemetry_service.application.ports.in.IngestTelemetryUseCase;
import com.streamhub.telemetry_service.application.ports.out.EventPublisherPort;
import com.streamhub.telemetry_service.domain.exception.ProfileNotSelectedException;
import com.streamhub.telemetry_service.domain.model.EventContext;
import com.streamhub.telemetry_service.domain.model.PingCommand;
import com.streamhub.telemetry_service.domain.model.RatingCommand;

/**
 * The service does not persist anything: it validates the profile and publishes the event.
 * The recommendation worker owns Cassandra and the Redis projection (ADR 002 and ADR 006).
 */
public class TelemetryIngestionService implements IngestTelemetryUseCase {

    private final EventPublisherPort publisher;

    public TelemetryIngestionService(EventPublisherPort publisher) {
        this.publisher = publisher;
    }

    @Override
    public void ingestPing(String profileId, PingCommand command, EventContext context) {
        requireProfile(profileId);
        publisher.publishPing(profileId, command, context);
    }

    @Override
    public void ingestRating(String profileId, RatingCommand command, EventContext context) {
        requireProfile(profileId);
        publisher.publishRating(profileId, command, context);
    }

    private static void requireProfile(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new ProfileNotSelectedException();
        }
    }
}
