package com.streamhub.telemetry_service.infrastructure.fixtures;

import com.streamhub.telemetry_service.application.ports.out.EventPublisherPort;
import com.streamhub.telemetry_service.domain.exception.ServiceUnavailableException;
import com.streamhub.telemetry_service.domain.model.EventContext;
import com.streamhub.telemetry_service.domain.model.PingCommand;
import com.streamhub.telemetry_service.domain.model.RatingCommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Records the events instead of publishing them, for the web tests. */
public class InMemoryEventPublisherAdapter implements EventPublisherPort {

    public record PublishedPing(String profileId, PingCommand command, EventContext context) {
    }

    public record PublishedRating(String profileId, RatingCommand command, EventContext context) {
    }

    private final List<PublishedPing> pings = new CopyOnWriteArrayList<>();
    private final List<PublishedRating> ratings = new CopyOnWriteArrayList<>();
    private final AtomicBoolean failNext = new AtomicBoolean(false);

    public void failNextPublication() {
        failNext.set(true);
    }

    public void clear() {
        pings.clear();
        ratings.clear();
        failNext.set(false);
    }

    public List<PublishedPing> pings() {
        return List.copyOf(pings);
    }

    public List<PublishedRating> ratings() {
        return List.copyOf(ratings);
    }

    @Override
    public void publishPing(String profileId, PingCommand command, EventContext context) {
        failIfRequested();
        pings.add(new PublishedPing(profileId, command, context));
    }

    @Override
    public void publishRating(String profileId, RatingCommand command, EventContext context) {
        failIfRequested();
        ratings.add(new PublishedRating(profileId, command, context));
    }

    private void failIfRequested() {
        if (failNext.getAndSet(false)) {
            throw new ServiceUnavailableException("The message broker");
        }
    }
}
