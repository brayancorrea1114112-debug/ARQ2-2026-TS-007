# ADR 002: Decoupled Ingestion of Playback Telemetry Using a Message Broker (RabbitMQ)

## Status
Accepted

## Context and Problem
Connected client devices emit an HTTP telemetry ping every 10 seconds. Processing these writes synchronously against the primary database blocks connections and makes simultaneous catalog browsing sluggish or entirely unavailable.

## Alternatives Considered
1. Direct synchronous writes to a PostgreSQL database.
2. Asynchronous ingestion through message queues using RabbitMQ.
3. Storage in the application server's local memory.

## Decision
Option 2 - Asynchronous ingestion using RabbitMQ.

## Trade-offs
- **Positive:** Immediate release of the HTTP thread with a `202 Accepted` response; complete isolation between catalog reads and telemetry ingestion; robust protection against crashes caused by traffic spikes.
- **Negative:** Additional operational complexity when managing the messaging cluster, exchanges, and consumers.

## Final Justification
It allows the system to absorb continuous write peaks from millions of active players without impacting the availability or responsiveness of the core browsing system.

## Note (complemented by ADR 006)
The broker remains the transport. Consumers persist each event in the Cassandra Interactions DB before projecting it into Redis, so no interaction data depends only on the cache.

## Implementation Notes

The producer is the `telemetry-service`:

- `POST /api/v1/telemetry/ping` and `POST /api/v1/interactions/like` verify the profile JWT and validate the `spec.md` body.
- It publishes `telemetry.ping.received` to `streamhub.telemetry` and `interaction.rating.added` to `streamhub.interactions`.
- It declares the same topic exchanges, durable queues, dead-letter queues and bindings as the recommendation worker, so messages wait when the worker is down.
- Publisher confirms are mandatory: the endpoint answers `202` only after the broker acknowledges, otherwise `503` and the client retries with the same `event_id`.
- Messages are persistent and carry `messageId=event_id`, `X-Correlation-ID` and the W3C `traceparent`.
- Kong exposes both routes with JWT validation and rate limiting.