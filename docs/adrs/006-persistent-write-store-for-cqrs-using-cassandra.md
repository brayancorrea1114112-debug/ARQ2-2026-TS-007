# ADR 006: Persistent Write Store for CQRS Using a Write-Optimized NoSQL Database (Cassandra)

## Status
Accepted

## Context and Problem
StreamHub separates the write path (playback pings, likes and ratings ingested through the Telemetry & Interactions Service and RabbitMQ) from the read path (the precomputed Home view served from Redis). However, in the current design the asynchronous workers consume events from the broker and write their result only to the Redis cache. This leaves the platform without a durable source of truth for user interaction data: Redis is an in-memory store and RabbitMQ is a transport, not a permanent record. If the cache is lost, evicted or corrupted, the "Continue Watching" state and the preference data used for recommendations cannot be rebuilt. In addition, a full CQRS implementation requires an authoritative write model from which read models are derived. Devices emit an HTTP ping every 10 seconds, so the chosen store must absorb a continuous, high-volume stream of small writes without affecting catalog reads.

## Alternatives Considered
1. Keep Redis as the only store for playback state and interactions, relying on Redis persistence (RDB/AOF).
2. Use a write-optimized NoSQL database (Cassandra) as the persistent write store, and project its data into Redis as the read model.
3. Reuse an existing database (PostgreSQL or MongoDB) to store playback progress and interactions.

## Decision
Option 2 - Introduce an Interactions DB based on a write-optimized NoSQL database (Cassandra) as the source of truth for watch progress, likes, ratings and raw interaction events. The Playback Progress Worker and the Recommendation Worker persist each processed event into this database and then project the resulting state into Redis, which remains a disposable read model.

## Trade-offs
- **Positive:** Durable source of truth for interaction data; Redis can be fully rebuilt from the Interactions DB after a failure or eviction; high write throughput suited to continuous 10-second pings; complete isolation from the catalog databases; a complete CQRS flow (command, event, persistence, read-model projection); raw events remain available for recalculating recommendations.
- **Negative:** One additional database to deploy, operate and monitor; dual write from the workers (persist, then project to Redis) requires idempotent consumers to handle redelivered messages; eventual consistency between the write store and the cache; the team must adopt data modeling practices specific to the chosen database (query-driven tables, partition keys).

## Final Justification
Persisting interaction events in a write-optimized store gives StreamHub a durable and rebuildable foundation for its read models, turning the current cache-aside and event-driven design into a complete CQRS architecture. It protects user data against cache loss and keeps the high-volume write workload separated from the catalog read path, which preserves the low-latency Home screen delivery defined in ADR 001 and the decoupled ingestion defined in ADR 002.
