package com.streamhub.telemetry_service.application.ports.in;

import com.streamhub.telemetry_service.domain.model.EventContext;
import com.streamhub.telemetry_service.domain.model.PingCommand;
import com.streamhub.telemetry_service.domain.model.RatingCommand;

public interface IngestTelemetryUseCase {

    void ingestPing(String profileId, PingCommand command, EventContext context);

    void ingestRating(String profileId, RatingCommand command, EventContext context);
}
