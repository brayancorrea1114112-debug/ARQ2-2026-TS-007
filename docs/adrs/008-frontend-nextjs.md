# ADR 008: Next.js Frontend Inside the Repository

## Status
Accepted

## Context and Problem
The platform has a documented HTTP contract (`docs/spec.md`) and an API Gateway (Kong), but no client application. The team needs a web client that exercises authentication, subprofile selection, the Home view, playback telemetry and Like/Dislike, and that can be shown before every backend service is finished. The client must not break the microservice architecture: it cannot become a new backend or reach the databases.

## Alternatives Considered
1. A separate repository for the frontend.
2. A `frontend/` folder in this repository.
3. A monorepo tool (Nx or Turborepo) with shared packages.

## Decision
Option 2. The client lives in `frontend/` and is built with Next.js (App Router), TypeScript and Tailwind CSS. It is a separate deployable, not a new backend.

The browser only calls `/api/v1`, and Next.js forwards those requests to Kong (`KONG_INTERNAL_URL`), so Kong stays the only entry point to the services. The client never talks to PostgreSQL, MongoDB, Redis, Cassandra or RabbitMQ, and it sends `Authorization: Bearer <JWT>` and `X-Correlation-ID` on every request. Telemetry pings and ratings follow the protocols of the spec and contain no business logic.

A development-only mock layer (MSW) in `src/mocks/` implements the same contracts as the spec. It is off by default (`NEXT_PUBLIC_USE_MOCKS=false`) and is only turned on for the offline demo and the end-to-end tests.

## Trade-offs
- Positive: one repository and atomic changes to the contract and the client; the client could be shown before the services existed; Kong remains the only backend entry point; no monorepo tool to maintain for a single frontend.
- Negative: the repository needs a clear boundary, so the client must not contain catalog, recommendation or persistence logic; the mock handlers must be kept in line with the spec, since they are development code and not a substitute for the services.

## Final Justification
Keeping the client in the same repository lets the team change the contract and its only consumer together, while the rule that the browser only talks to Kong keeps the client outside the backend. The mock layer allowed the frontend to advance in parallel with the services without changing the contract.
