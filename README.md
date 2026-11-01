# StreamHub VOD Platform - Software Architecture (Team 007)

Welcome to the official repository for **StreamHub VOD**, a high-performance Video-on-Demand (VOD) software platform designed to handle heavy concurrent read traffic, asynchronous telemetry ingestion, and personalized content delivery.

## Project Overview

StreamHub is built using a modern microservices architecture following **Domain-Driven Design (DDD)** principles and **Hexagonal Architecture**. It addresses critical scalability bottlenecks during peak hours (such as 8:00 PM user rushes) through decoupled telemetry processing, efficient in-memory caching, and document-oriented catalog management.

## Documentation Link
**Docs Link:** https://docs.google.com/document/d/14LykQcHYPvamaiVPFFPQfhcVMt8RC_OaAPY49MoZNDs/edit?tab=t.0

## Interactions DB (Cassandra)

The `recommendation-worker` persists playback progress, ratings and raw events in Cassandra
before projecting them into Redis. Cassandra is the durable source of truth (ADR 006); Redis
can be rebuilt from it at any time.

```bash
# Start Cassandra, create the keyspace/tables and start the worker
docker compose up -d cassandra cassandra-init redis rabbitmq recommendation-worker
```

Schema: `infra/cassandra/init.cql` (keyspace `streamhub`).

Relevant environment variables:

| Variable | Default | Description |
|---|---|---|
| `CASSANDRA_HOST` | `cassandra` | Contact point |
| `CASSANDRA_PORT` | `9042` | Native transport port |
| `CASSANDRA_DATACENTER` | `datacenter1` | Local datacenter |
| `CASSANDRA_KEYSPACE` | `streamhub` | Keyspace |
| `CASSANDRA_HISTORY_TTL_DAYS` | `90` | Retention of raw event history |
| `ADMIN_TOKEN` | `changeme` | Token for the rebuild endpoints |
| `REBUILD_ON_STARTUP` | `false` | Rebuild the read model on every start |

Rebuild the Redis read model from Cassandra:

```bash
# One profile
curl -X POST http://localhost:8082/admin/rebuild/prof_9921 -H "X-Admin-Token: streamhub-admin-token"

# All profiles registered in Cassandra
curl -X POST http://localhost:8082/admin/rebuild -H "X-Admin-Token: streamhub-admin-token"
```

Tests: `mvn -q -B test` inside `services/recommendation-worker`.


## Repository Team

- **Team Name:** Team 007
- **Course:** Software Architecture II
- **Members:** Juan Jose Rua David, Juan José Alcaraz Guisao, Brayan Alexis Correa Torres, Luis Alfredo Taborda Lopez, Kevin Cardenas Rivillas
