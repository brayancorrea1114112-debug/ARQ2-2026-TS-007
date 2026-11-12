# ADR 007: TTL-Based Cache Refresh from the Interactions DB and Idempotent Event Processing

## Status
Accepted (amends ADR 006)

## Context and Problem
ADR 006 introduced the Interactions DB (Cassandra) as the source of truth for user interactions and defined that the Playback Progress Worker and the Recommendation Worker persist each event there and then project the resulting state into Redis. This dual write cannot be wrapped in a single transaction, so a failure between both steps leaves Redis out of sync, and it requires additional mechanisms (ordering checks and reconciliation) to keep both stores aligned. It also turns Redis into a near-complete copy of the Interactions DB, while the role of the cache is to hold only the data that is requested frequently. In addition, RabbitMQ delivers messages at least once, so workers can receive the same event more than once.

## Alternatives Considered
1. Keep the dual write defined in ADR 006: workers persist events in Cassandra and project the result into Redis.
2. Workers write only to Cassandra, and a scheduled job projects recent changes into Redis.
3. Workers write only to Cassandra, and Redis is refreshed through Cache-Aside with TTL: on a cache miss, the Catalog & Home Service rebuilds the entry from MongoDB and the Interactions DB.

## Decision
Option 3 - Workers no longer write to Redis. This replaces the projection step defined in ADR 006; the rest of ADR 006 remains in force.
- Workers: they only persist events in the Interactions DB. Events carry a client-generated `eventId` and describe absolute state (for example, "position = minute 34"). Writes are upserts keyed by `eventId` and use the event timestamp as the write timestamp, so redelivered messages produce the same result and the most recent event wins when messages arrive out of order. Messages that fail repeatedly are moved to a dead-letter queue after a configurable number of attempts.
- Redis: entries expire by TTL. The Home with its recommendations and Continue Watching, which is stored under a separate key, both use a TTL. A small random variation is applied to each TTL to avoid mass expirations at the same moment.
- Cache miss: the Catalog & Home Service builds the Home from MongoDB, reads the viewer's progress and recommendations from the Interactions DB, stores the result in Redis with its TTL and returns it. Only one request rebuilds a given key at a time; concurrent requests for the same key wait for that result.
- Recommendations: the Recommendation Worker persists its computed recommendations in the Interactions DB so that the Catalog & Home Service can read them on a cache miss.

## Trade-offs
- Positive: A single write per event, with no dual write and no scheduled job or additional infrastructure; Redis only holds data that is actually requested; the Interactions DB remains the single source of truth; safe reprocessing of redelivered or out-of-order messages.
- Negative: The first request after each expiration pays the cost of rebuilding the entry, which affects the latency of the initial Home load; many simultaneous expirations could overload MongoDB and Cassandra, which is mitigated with TTL variation and single-request rebuilds; consistency is eventual and bounded by the TTL; the Catalog & Home Service now also depends on the Interactions DB for reads.

## Final Justification
Removing the projection from the workers simplifies the design of ADR 006 by eliminating the non-atomic dual write, while the Interactions DB keeps its role as the durable source of truth. Redis stays a disposable cache refreshed on demand following the Cache-Aside approach of ADR 001, and its staleness is bounded by explicit TTLs. Idempotent upserts make the at-least-once delivery of RabbitMQ (ADR 002) safe without additional coordination between stores.
