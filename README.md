# StreamHub VOD Platform - Software Architecture (Team 007)

Welcome to the official repository for **StreamHub VOD**, a high-performance Video-on-Demand (VOD) software platform designed to handle heavy concurrent read traffic, asynchronous telemetry ingestion, and personalized content delivery.

## Project Overview

StreamHub is built using a modern microservices architecture following **Domain-Driven Design (DDD)** principles and **Hexagonal Architecture**. It addresses critical scalability bottlenecks during peak hours (such as 8:00 PM user rushes) through decoupled telemetry processing, efficient in-memory caching, and document-oriented catalog management.

## Documentation Link
**Docs Link:** https://docs.google.com/document/d/14LykQcHYPvamaiVPFFPQfhcVMt8RC_OaAPY49MoZNDs/edit?tab=t.0

## Frontend (Next.js)

The web client lives in `frontend/` and talks to the platform only through Kong
(`/api/v1`). It runs against the real services by default
(`NEXT_PUBLIC_USE_MOCKS=false`). For the offline demo used by the mock E2E suite,
set `NEXT_PUBLIC_USE_MOCKS=true` and `NEXT_PUBLIC_MOCK_SCOPE=all`.

```bash
cd frontend
npm install
npm run dev   # http://localhost:3001
```

Create the root `.env` from `.env.example` (`JWT_SECRET` and `MONGODB_URI`) and
start the stack. `mongo-seed` loads the development catalog; Home cache entries
are built on demand from MongoDB and Cassandra:

```bash
docker compose up -d --build postgres auth-service mongo mongo-seed \
  redis catalog-service telemetry-service \
  rabbitmq cassandra cassandra-init recommendation-worker \
  otel-collector jaeger kong
```

Demo accounts: `ana@example.com` / `S3cret-pass` (Ana and Sam profiles) and
`luis@example.com` / `S3cret-pass`. See `frontend/README.md` for the environment
flags, scripts and the E2E modes.

## Telemetry, interactions and Jaeger

`telemetry-service` serves `POST /api/v1/telemetry/ping` and
`POST /api/v1/interactions/like`, verifies the profile JWT and publishes
`telemetry.ping.received` / `interaction.rating.added` to RabbitMQ with publisher
confirms. The `recommendation-worker` consumes them and persists interaction
state plus computed recommendations to Cassandra. Catalog & Home fills Redis on
cache misses (ADR 002, ADR 006 and ADR 007).

Traces are collected with OpenTelemetry and exported to **Jaeger** at
<http://localhost:16686>. A ping trace follows Kong → telemetry-service →
RabbitMQ → recommendation-worker. Prometheus, Loki, Grafana and dashboards are
deliberately out of this delivery (ADR 005).

## Interactions DB (Cassandra)

The `recommendation-worker` persists playback progress, ratings, raw events and computed
recommendations in Cassandra. Cassandra is the durable source of truth (ADR 006, amended by
ADR 007); workers do not write to Redis. On a Home cache miss, Catalog & Home reads trending
titles from MongoDB and progress/recommendations from Cassandra, then caches the result.

```bash
# Start Cassandra, create the keyspace/tables and start the worker
docker compose up -d cassandra cassandra-init redis rabbitmq recommendation-worker catalog-service
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
| `HOME_CACHE_TTL` | `15m` | Redis Home entry TTL |
| `CONTINUE_WATCHING_CACHE_TTL` | `24h` | Separate Continue Watching entry TTL |
| `CACHE_TTL_JITTER` | `2m` | Random TTL variation to spread expirations |

Tests: `mvn -q -B test` inside `services/recommendation-worker`.


## Repository Team

- **Team Name:** Team 007
- **Course:** Software Architecture II
- **Members:** Juan Jose Rua David, Juan José Alcaraz Guisao, Brayan Alexis Correa Torres, Luis Alfredo Taborda Lopez, Kevin Cardenas Rivillas
