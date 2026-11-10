package com.streamhub.telemetry_service.application.ports.out;

import com.streamhub.telemetry_service.domain.model.EventContext;
import com.streamhub.telemetry_service.domain.model.PingCommand;
import com.streamhub.telemetry_service.domain.model.RatingCommand;

public interface EventPublisherPort {

    void publishPing(String profileId, PingCommand command, EventContext context);

    void publishRating(String profileId, RatingCommand command, EventContext context);
}
