# ADR 002: Decoupled Ingestion of Playback Telemetry Using a Message Broker (RabbitMQ)

## Status
Accepted (complemented by ADR 006 and amended by ADR 007)

## Context and Problem
Connected client devices emit an HTTP telemetry ping every 10 seconds. Processing these writes synchronously against the primary database blocks connections and makes simultaneous catalog browsing sluggish or entirely unavailable.

## Alternatives Considered
1. Direct synchronous writes to a PostgreSQL database.
2. Asynchronous ingestion through message queues using RabbitMQ.
3. Storage in the application server's local memory.

## Decision
Option 2 - Asynchronous ingestion using RabbitMQ.

The Telemetry Service receives `POST /api/v1/telemetry/ping` and `POST /api/v1/interactions/like`, validates the body and the profile token, and publishes `telemetry.ping.received` to the `streamhub.telemetry` exchange and `interaction.rating.added` to the `streamhub.interactions` exchange. It answers `202` only after the broker confirms the message (publisher confirms); otherwise it answers `503` and the client retries with the same `event_id`. Messages are persistent and carry the `event_id`, the `X-Correlation-ID` and the `traceparent`. The queues are durable and have a dead-letter queue, so messages wait while the worker is down. The worker stores the result in the Interactions DB (ADR 006) and does not write to Redis (ADR 007).

## Trade-offs
- Positive: Immediate release of the HTTP thread with a `202 Accepted` response; complete isolation between catalog reads and telemetry ingestion; robust protection against crashes caused by traffic spikes.
- Negative: Additional operational complexity when managing the messaging cluster, exchanges, and consumers.

## Final Justification
It allows the system to absorb continuous write peaks from millions of active players without impacting the availability or responsiveness of the core browsing system.
