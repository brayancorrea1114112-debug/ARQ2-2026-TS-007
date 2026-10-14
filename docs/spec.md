# StreamHub VOD - Contract Specification (spec.md)

Team 007 - Software Architecture II - Case 7: StreamHub - VOD Catalog and Recommendations

This file is the contract for Wave 1 and the reference against which the Wave 2 code will be reviewed. It is consistent with the Tech Spec (Google Doc `TS-007`), the ADRs in `/docs/adrs` and the C4 container diagram in `/docs/assets`.

## Table of Contents

1. [Overview](#1-overview)
2. [Data Schemas](#2-data-schemas)
3. [HTTP APIs](#3-http-apis)
4. [Asynchronous Events](#4-asynchronous-events)
5. [Requirements and Scenarios](#5-requirements-and-scenarios)
6. [Open Points for Wave 2](#6-open-points-for-wave-2)

---

## 1. Overview

### 1.1 Problem being solved

At 8:00 PM the "Home" screen is built with real-time JOINs between users, viewing history and the catalog, which takes up to 10 seconds per user. In addition, every client sends a playback "ping" every 10 seconds, and these constant writes saturate the same database connection pool used by the catalog.

The solution has two rules:

- Reads of the Home screen never touch a relational database: they are served from a precomputed view in Redis.
- Playback pings and likes are never written synchronously: they are published to RabbitMQ and processed in the background.

### 1.2 Services and responsibilities

| Service | Container | Responsibility |
|---|---|---|
| **API Gateway** | Kong | Single entry point. Validates the JWT, applies rate limiting, routes requests, and creates the `X-Correlation-ID` and the W3C `traceparent` headers when the client does not send them. |
| **Auth & Profile Service** | Container + PostgreSQL | Master accounts, login, family subprofiles and subprofile selection. Issues the JWTs. |
| **Catalog & Home Service** | Container + Redis + MongoDB | Serves the Home screen from Redis. Falls back to MongoDB when the view is not in the cache. Exposes catalog metadata. |
| **Telemetry Service** | Container + RabbitMQ | Receives playback pings and likes, validates them, publishes them as events and replies `202 Accepted` immediately. It does not write to any database. |
| **Message Broker** | RabbitMQ | Decouples ingestion from processing. Holds the telemetry and interaction queues and their dead-letter queues. |
| **Recommendation Worker** | Container | Consumes the queues. Updates "Continue Watching" in Redis, updates the profile preferences and refreshes the precomputed Home view. |
| **Observability Stack** | OpenTelemetry, Jaeger, Prometheus, Loki, Grafana | Collects traces, metrics and logs to locate latency in the initial requests. |

### 1.3 Cross-cutting rules

- **Authentication:** every endpoint except `POST /api/v1/auth/login` requires `Authorization: Bearer <JWT>`. The Gateway validates the signature and expiration before forwarding the request.
- **Two token types:** an *account token* (claim `sub` = account id) is returned by login. A *profile token* (claims `sub` = account id and `profile_id`) is returned by profile selection. Endpoints for Home, telemetry and interactions require a profile token.
- **Correlation:** every request carries `X-Correlation-ID` (UUID v4). The Gateway generates it if missing. It is propagated through HTTP calls, copied into the headers of every published event, and written in every log line.
- **Tracing:** services propagate the W3C `traceparent` header and export spans with OpenTelemetry to Jaeger.
- **Content type:** `application/json; charset=utf-8`. Timestamps use ISO 8601 UTC.

### 1.4 Standard error body

All 4xx and 5xx responses use this body:

```json
{
  "error": {
    "code": "PROFILE_NOT_FOUND",
    "message": "The profile does not exist or does not belong to this account.",
    "correlation_id": "5b9e1c1e-6f0a-4d6b-9f55-6c0f1f3a7d11"
  }
}
```

---

## 2. Data Schemas

### 2.1 Account (PostgreSQL, Auth DB)

| Field | Type | Notes |
|---|---|---|
| `account_id` | UUID | Primary key |
| `email` | string | Unique |
| `password_hash` | string | Never returned by the API |
| `created_at` | timestamp | |

### 2.2 Profile (PostgreSQL, Auth DB)

| Field | Type | Notes |
|---|---|---|
| `profile_id` | string | Primary key, e.g. `prof_9921` |
| `account_id` | UUID | Foreign key to Account |
| `name` | string | |
| `type` | enum | `ADULT` or `KIDS` |
| `avatar_url` | string | |
| `created_at` | timestamp | |

### 2.3 Title (MongoDB, Catalog DB, collection `titles`)

```json
{
  "_id": "mov_8832",
  "type": "SERIES",
  "name": "Example Series",
  "genres": ["Drama", "Thriller"],
  "cast": ["Actor One", "Actor Two"],
  "age_rating": "PG-13",
  "images": { "poster": "https://cdn.example.com/p/mov_8832.jpg" },
  "seasons": [
    {
      "number": 1,
      "episodes": [
        { "episode_id": "ep_101", "title": "Pilot", "duration_seconds": 3000 },
        { "episode_id": "ep_102", "title": "Second", "duration_seconds": 2900 }
      ]
    }
  ]
}
```

A movie has no `seasons` and uses a single `duration_seconds` at the top level.

### 2.4 HomeView (Redis, key `home:{profile_id}`)

Precomputed JSON document. TTL: 15 minutes. It is rebuilt by the Recommendation Worker after relevant events and invalidated when the catalog changes.

```json
{
  "profile_id": "prof_9921",
  "generated_at": "2026-10-06T19:58:00Z",
  "categories": [
    { "name": "Trending", "title_ids": ["mov_1", "mov_2"] }
  ],
  "continue_watching": [
    { "content_id": "mov_8832", "episode_id": "ep_102", "progress_seconds": 1420 }
  ],
  "recommended": ["mov_77", "mov_91"]
}
```

### 2.5 PlaybackProgress (Redis, key `progress:{profile_id}:{content_id}`)

| Field | Type | Notes |
|---|---|---|
| `episode_id` | string | Optional, only for series |
| `progress_seconds` | integer | Last accepted position |
| `last_event_timestamp` | timestamp | Used to ignore out-of-order events |

### 2.6 Rating (event payload and preference input)

| Field | Type | Notes |
|---|---|---|
| `profile_id` | string | |
| `content_id` | string | |
| `rating` | enum | `LIKE` or `DISLIKE` |
| `timestamp` | timestamp | The latest rating for the same profile and title wins |

### 2.7 ProfilePreferences (Redis, key `prefs:{profile_id}`)

Preference vector (genre weights) recalculated by the Recommendation Worker from ratings and watch history. It is the input to the `recommended` list of the HomeView.

---

## 3. HTTP APIs

Base path: `/api/v1`. All requests go through the API Gateway.

Common responses for every authenticated endpoint:

| Status | When |
|---|---|
| 401 | Missing, malformed, invalid or expired JWT (`UNAUTHORIZED`) |
| 429 | Rate limit exceeded (`RATE_LIMITED`). Includes `Retry-After` |
| 500 | Unexpected error (`INTERNAL_ERROR`) |
| 503 | A required dependency is unavailable (`SERVICE_UNAVAILABLE`) |

### 3.1 POST /api/v1/auth/login

Authenticates a master account.

- **Authentication:** none (public)
- **Service:** Auth & Profile Service

Request body:

```json
{ "email": "ana@example.com", "password": "S3cret-pass" }
```

Responses:

| Status | Description |
|---|---|
| 200 | Login successful. Returns an account token |
| 400 | Invalid body (`VALIDATION_ERROR`) |
| 401 | Wrong credentials (`INVALID_CREDENTIALS`) |
| 429 | Too many attempts |
| 5xx | Server error |

200 example:

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "account": { "account_id": "9c3d1f6e-1b0a-4f64-a1d1-0a8f5a0b7c21", "email": "ana@example.com" }
}
```

### 3.2 GET /api/v1/profiles

Lists the subprofiles of the authenticated account.

- **Authentication:** Bearer account token
- **Service:** Auth & Profile Service

Responses:

| Status | Description |
|---|---|
| 200 | List of profiles |
| 401 | Invalid or expired token |
| 5xx | Server error |

200 example:

```json
{
  "profiles": [
    { "profile_id": "prof_9921", "name": "Ana", "type": "ADULT", "avatar_url": "https://cdn.example.com/a/1.png" },
    { "profile_id": "prof_9922", "name": "Sam", "type": "KIDS", "avatar_url": "https://cdn.example.com/a/2.png" }
  ]
}
```

### 3.3 POST /api/v1/profiles/select

Selects the active subprofile and returns a profile token.

- **Authentication:** Bearer account token
- **Service:** Auth & Profile Service

Request body:

```json
{ "profile_id": "prof_9921" }
```

Responses:

| Status | Description |
|---|---|
| 200 | Profile selected. Returns a token that includes the `profile_id` claim |
| 400 | Missing or invalid `profile_id` |
| 401 | Invalid or expired token |
| 403 | The profile belongs to another account (`PROFILE_FORBIDDEN`) |
| 404 | The profile does not exist (`PROFILE_NOT_FOUND`) |
| 5xx | Server error |

200 example:

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "profile": { "profile_id": "prof_9921", "name": "Ana", "type": "ADULT" }
}
```

### 3.4 GET /api/v1/home

Returns the Home screen of the active profile. It is served from Redis and never runs JOINs.

- **Authentication:** Bearer profile token
- **Service:** Catalog & Home Service
- **Target latency:** under 20 ms on a cache hit

Responses:

| Status | Description |
|---|---|
| 200 | Home screen. `source` is `cache` or `fallback` |
| 401 | Invalid or expired token |
| 403 | The token has no `profile_id` claim (`PROFILE_NOT_SELECTED`) |
| 503 | Redis and MongoDB are both unavailable |
| 5xx | Server error |

200 example:

```json
{
  "profile_id": "prof_9921",
  "generated_at": "2026-10-06T19:58:00Z",
  "source": "cache",
  "categories": [
    { "name": "Trending", "titles": [ { "content_id": "mov_1", "name": "Title One", "poster": "https://cdn.example.com/p/mov_1.jpg" } ] }
  ],
  "continue_watching": [
    { "content_id": "mov_8832", "episode_id": "ep_102", "progress_seconds": 1420 }
  ],
  "recommended": [
    { "content_id": "mov_77", "name": "Title 77", "poster": "https://cdn.example.com/p/mov_77.jpg" }
  ]
}
```

On a cache miss the service answers from MongoDB with a generic Home (trending titles and the profile's progress when available) and returns `"source": "fallback"`. It never builds recommendations with JOINs.

### 3.5 POST /api/v1/telemetry/ping

Reports the current playback position. Clients send it every 10 seconds.

- **Authentication:** Bearer profile token
- **Service:** Telemetry Service
- **Behavior:** validates the body, publishes `telemetry.ping.received` and answers right away. It does not wait for any worker or database.

Request body:

| Field | Type | Required | Notes |
|---|---|---|---|
| `event_id` | UUID v4 | yes | Generated by the client. Used for idempotency, so a retry reuses the same value |
| `content_id` | string | yes | |
| `episode_id` | string | no | Only for series |
| `progress_seconds` | integer | yes | 0 or greater |
| `timestamp` | timestamp | yes | Device time of the ping |

```json
{
  "event_id": "0f6c2a52-6a53-4f7e-8a56-9d8c6a6b5f10",
  "content_id": "mov_8832",
  "episode_id": "ep_102",
  "progress_seconds": 1420,
  "timestamp": "2026-10-06T20:00:00Z"
}
```

Responses:

| Status | Description |
|---|---|
| 202 | Accepted for background processing |
| 400 | Invalid body (negative progress, bad UUID, missing field) |
| 401 | Invalid or expired token |
| 403 | The token has no `profile_id` claim |
| 429 | Rate limit exceeded |
| 503 | The broker did not confirm the publication. The client may retry with the same `event_id` |
| 5xx | Server error |

202 example:

```json
{ "event_id": "0f6c2a52-6a53-4f7e-8a56-9d8c6a6b5f10", "status": "ACCEPTED" }
```

### 3.6 POST /api/v1/interactions/like

Registers a Like or Dislike for a title.

- **Authentication:** Bearer profile token
- **Service:** Telemetry Service (publishes the event)

Request body:

```json
{
  "event_id": "a3b1b8d4-2c9f-4a53-b2f5-0a5c8e4d7a90",
  "content_id": "mov_8832",
  "rating": "LIKE",
  "timestamp": "2026-10-06T20:01:15Z"
}
```

`rating` accepts `LIKE` or `DISLIKE`.

Responses:

| Status | Description |
|---|---|
| 202 | Accepted for background processing |
| 400 | Invalid body or invalid `rating` value |
| 401 | Invalid or expired token |
| 403 | The token has no `profile_id` claim |
| 429 | Rate limit exceeded |
| 503 | The broker did not confirm the publication |
| 5xx | Server error |

202 example:

```json
{ "event_id": "a3b1b8d4-2c9f-4a53-b2f5-0a5c8e4d7a90", "status": "ACCEPTED" }
```

---

## 4. Asynchronous Events

Broker: RabbitMQ. Both exchanges are `topic`, durable. Both queues are durable with a dead-letter queue.

Headers on every event: `X-Correlation-ID`, `traceparent`, `content_type: application/json`, `delivery_mode: 2` (persistent).

### 4.1 telemetry.ping.received

| Item | Value |
|---|---|
| **Name** | `telemetry.ping.received` |
| **Producer** | Telemetry Service |
| **Consumers** | Recommendation Worker |
| **Exchange** | `streamhub.telemetry` |
| **Routing key** | `telemetry.ping.received` |
| **Queue** | `telemetry.progress.queue` (dead-letter queue: `telemetry.progress.dlq`) |

Payload schema:

| Field | Type | Required |
|---|---|---|
| `event_id` | UUID v4 | yes |
| `profile_id` | string | yes |
| `content_id` | string | yes |
| `episode_id` | string | no |
| `progress_seconds` | integer | yes |
| `timestamp` | timestamp | yes |

```json
{
  "event_id": "0f6c2a52-6a53-4f7e-8a56-9d8c6a6b5f10",
  "profile_id": "prof_9921",
  "content_id": "mov_8832",
  "episode_id": "ep_102",
  "progress_seconds": 1420,
  "timestamp": "2026-10-06T20:00:00Z"
}
```

Delivery guarantees:

- **Delivery:** at-least-once. The producer uses publisher confirms. The consumer uses manual acknowledgment after processing.
- **Idempotency:** the worker stores `event_id` in Redis with a 24-hour TTL. A duplicate is acknowledged and ignored.
- **Ordering:** global ordering is not guaranteed. The worker updates the progress only if `timestamp` is newer than `last_event_timestamp` for that profile and title, so an old ping never overwrites a newer one.
- **Retries:** up to 3 attempts with exponential backoff (1 s, 5 s, 30 s). After that the message goes to `telemetry.progress.dlq` for inspection.

### 4.2 interaction.rating.added

| Item | Value |
|---|---|
| **Name** | `interaction.rating.added` |
| **Producer** | Telemetry Service |
| **Consumers** | Recommendation Worker |
| **Exchange** | `streamhub.interactions` |
| **Routing key** | `interaction.rating.added` |
| **Queue** | `interactions.rating.queue` (dead-letter queue: `interactions.rating.dlq`) |

Payload schema:

| Field | Type | Required |
|---|---|---|
| `event_id` | UUID v4 | yes |
| `profile_id` | string | yes |
| `content_id` | string | yes |
| `rating` | enum `LIKE` / `DISLIKE` | yes |
| `timestamp` | timestamp | yes |

```json
{
  "event_id": "a3b1b8d4-2c9f-4a53-b2f5-0a5c8e4d7a90",
  "profile_id": "prof_9921",
  "content_id": "mov_8832",
  "rating": "LIKE",
  "timestamp": "2026-10-06T20:01:15Z"
}
```

Delivery guarantees:

- **Delivery:** at-least-once, with publisher confirms and manual acknowledgment.
- **Idempotency:** deduplicated by `event_id`, same as telemetry.
- **Ordering:** for the same `profile_id` and `content_id`, the rating with the newest `timestamp` wins.
- **Retries:** same policy as telemetry, then `interactions.rating.dlq`.

### 4.3 What the worker does after each event

| Event | Worker action |
|---|---|
| `telemetry.ping.received` | Update `progress:{profile_id}:{content_id}` in Redis and refresh the `continue_watching` section of `home:{profile_id}` |
| `interaction.rating.added` | Update `prefs:{profile_id}`, recalculate the recommendations and refresh `home:{profile_id}` |

---

## 5. Requirements and Scenarios

### Feature 1: Master account authentication with family subprofile selection

- **WHEN** a user sends valid credentials to `POST /api/v1/auth/login` **THEN** the system returns `200` with an account token.
- **WHEN** a user sends a wrong password **THEN** the system returns `401` with `INVALID_CREDENTIALS`.
- **WHEN** an authenticated account calls `GET /api/v1/profiles` **THEN** the system returns only the profiles of that account.
- **WHEN** an account selects one of its profiles with `POST /api/v1/profiles/select` **THEN** the system returns `200` and a token that contains `profile_id`.
- **WHEN** an account selects a profile that belongs to another account **THEN** the system returns `403`.
- **WHEN** a request arrives at the Gateway with an expired or invalid JWT **THEN** the Gateway returns `401` and the request never reaches the services.

### Feature 2: Home screen in milliseconds from cache

- **WHEN** a client calls `GET /api/v1/home` and the view exists in Redis **THEN** the system returns `200` with `source: "cache"` in under 20 ms and runs no relational query.
- **WHEN** the view is not in Redis **THEN** the system answers from MongoDB with `source: "fallback"` and does not run JOINs.
- **WHEN** Redis and MongoDB are both down **THEN** the system returns `503` with a standard error body.
- **WHEN** the token has no `profile_id` **THEN** the system returns `403` with `PROFILE_NOT_SELECTED`.
- **WHEN** a heavy load of pings is being processed **THEN** `GET /api/v1/home` keeps the same latency, because ingestion does not use the catalog connections.

### Feature 3: Massive, decoupled telemetry ingestion

- **WHEN** a client sends `POST /api/v1/telemetry/ping` with a valid body **THEN** the Telemetry Service publishes `telemetry.ping.received` and returns `202` without waiting for any worker or database.
- **WHEN** the same `event_id` is received twice **THEN** the worker processes it once.
- **WHEN** a ping with an older `timestamp` is processed after a newer one **THEN** the stored progress does not change.
- **WHEN** the worker fails to process a message 3 times **THEN** the message is moved to `telemetry.progress.dlq`.
- **WHEN** RabbitMQ does not confirm the publication **THEN** the service returns `503` and the client retries with the same `event_id`.
- **WHEN** the Recommendation Worker is down **THEN** the API keeps returning `202` and the messages wait in the queue until the worker is back.

### Feature 4: Background processing of Like / Dislike

- **WHEN** a client sends `POST /api/v1/interactions/like` with `LIKE` or `DISLIKE` **THEN** the system returns `202` and publishes `interaction.rating.added`.
- **WHEN** the worker consumes the event **THEN** it updates the profile preferences and refreshes the recommendations in the Home view.
- **WHEN** a profile rates the same title twice **THEN** the rating with the newest `timestamp` is the one used.
- **WHEN** the `rating` value is not `LIKE` or `DISLIKE` **THEN** the system returns `400`.

### Feature 5: Traceability of latency in the initial requests

- **WHEN** a request enters the Gateway without `X-Correlation-ID` **THEN** the Gateway generates one and forwards it to every service.
- **WHEN** a request crosses Gateway, Catalog & Home Service and Redis or MongoDB **THEN** Jaeger shows a single trace with one span per hop and its duration.
- **WHEN** an event is published **THEN** its headers carry `X-Correlation-ID` and `traceparent`, so the worker spans join the trace of the original request.
- **WHEN** the Home screen is slow **THEN** an engineer can search Jaeger or Loki by `correlation_id` and find the component that adds the latency.
- **WHEN** any error response is returned **THEN** its body includes the `correlation_id`.

---

## 6. Open Points for Wave 2

These points are not defined yet and will be settled with an ADR or in this file before the implementation:

- **Durability of progress and ratings:** the C4 diagram keeps them in Redis. Redis will run with AOF persistence, and ratings may also be stored in MongoDB. This will be confirmed in Wave 2.
- **Programming language of each service:** to be decided in Wave 2.
- **Rate limit values and token lifetime:** the values shown here (3600 s token) are the initial proposal.
