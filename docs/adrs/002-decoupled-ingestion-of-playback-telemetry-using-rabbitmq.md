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