# ADR 001: Use of Redis for Caching and Rendering the "Home" Screen

## Status
Accepted

## Context and Problem
At 8:00 PM, thousands of users open the StreamHub application simultaneously. Executing JOINs to build recommendations, the catalog, and "Continue Watching" directly in the relational database saturates the connection pool and takes up to 10 seconds per request, resulting in poor user experience and potential system instability.

## Alternatives Considered
1. Optimized queries using materialized views in PostgreSQL.
2. Implementation of an in-memory cache with Redis (Cache-Aside / Pre-computed Read View Pattern).
3. Direct storage of views in Elasticsearch.

## Decision
Option 2 - Use Redis to store the precomputed Home view and the current playback state for each subprofile.

## Trade-offs
- **Positive:** Sub-10ms read latency; complete independence from the relational database; massive absorption of synchronous reads and pings in memory.
- **Negative:** Eventual data inconsistency if the catalog is updated (mitigated through TTL-based expiration policies and invalidation events).

## Final Justification
This is the standard pattern for VOD platforms that need to handle intensive read workloads with minimal latency during peak traffic hours.