# ADR 008: Next.js frontend inside the repository

## Status

Accepted

## Context and Problem

The platform has a documented HTTP contract (`docs/spec.md`) and an API Gateway
(Kong), but no client application. The team needs a web client that exercises
authentication, subprofile selection, the Home view, playback telemetry and
Like/Dislike without waiting for the Catalog & Home and Telemetry services to be
implemented.

Two questions had to be settled: where the client lives and how it is built
without violating the microservice architecture.

## Alternatives Considered

1. A separate repository for the frontend.
2. A `frontend/` folder in this repository.
3. A full monorepo tool (Nx/Turborepo) with shared packages.

## Decision

Option 2. The client lives in `frontend/` and is built with Next.js (App Router),
TypeScript and Tailwind CSS. It is a separate deployable/container, not a new
backend.

The missing services are represented by a development-only mock layer (MSW) that
implements the exact contracts in `docs/spec.md`:

- `POST /api/v1/auth/login`
- `GET /api/v1/profiles`
- `POST /api/v1/profiles/select`
- `GET /api/v1/home`
- `POST /api/v1/telemetry/ping`
- `POST /api/v1/interactions/like`

`NEXT_PUBLIC_USE_MOCKS=false` disables the worker and points the client at Kong.
The mock layer stays in the repository but is never active in production.

## Consequences

Positive:

- One repository, one team, atomic changes to contract and client.
- The client can be demonstrated before the two missing services exist.
- The architecture keeps Kong as the only backend entry point.
- No Nx/Turborepo overhead for a single frontend.

Negative:

- A monorepo needs a clear boundary: the client must not contain catalog,
  recommendation or persistence logic.
- The mock handlers must be maintained next to the spec; they are development
  code, not a substitute for the services.

## Architecture rules for the client

- It is a container in the C4 model: browser → Web App → Kong → services.
- It never talks to PostgreSQL, MongoDB, Redis, Cassandra or RabbitMQ.
- It sends `Authorization: Bearer <JWT>` and `X-Correlation-ID` on every request.
- Telemetry pings and ratings are client protocols defined by the spec, not
  business logic.
- Mocks are isolated under `src/mocks/` and gated by an environment flag.

## Integration checklist

When the Catalog & Home Service and the Telemetry Service exist:

1. Set `NEXT_PUBLIC_USE_MOCKS=false`.
2. Add the `home`, `telemetry` and `interactions` routes to `kong/kong.yml`.
3. Add the Kong CORS plugin for `http://localhost:3001`, allowing
   `Authorization`, `Content-Type` and `X-Correlation-ID`.
4. The Home service must expand `recommended` ids and `categories[].title_ids`
   into the object shape documented in `docs/spec.md` section 3.4.

## Update: catalog-service integration

`catalog-service` now implements `GET /api/v1/home`, is routed by Kong and has a
local MongoDB seed (`infra/mongo/seed.js`). Kong now exposes a global
`cors` plugin for `http://localhost:3001`.

The client therefore defaults to a partial mock mode
(`NEXT_PUBLIC_MOCK_SCOPE=missing`): login, profiles and home go to Kong, while
playback telemetry and likes stay mocked until the Telemetry Service exists.
`NEXT_PUBLIC_MOCK_SCOPE=all` keeps the fully offline demo used by the E2E suite.
The Catalog & Home Service currently returns summaries only (`content_id`, `name`,
`poster`), so synopsis, duration and genres are still absent from the UI until a
title-detail endpoint exists.

## Update: telemetry service and Jaeger

The Telemetry & Interactions Service now exists and is routed by Kong, so the
client runs with `NEXT_PUBLIC_USE_MOCKS=false` by default. The offline demo and
the mock E2E suite use `NEXT_PUBLIC_USE_MOCKS=true` with
`NEXT_PUBLIC_MOCK_SCOPE=all`.

Jaeger tracing is enabled for the gateway, auth, catalog, telemetry and the
worker (traces only). The trace of a ping follows Kong → telemetry-service →
RabbitMQ → recommendation-worker.
