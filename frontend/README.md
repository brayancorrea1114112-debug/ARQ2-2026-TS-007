# StreamHub — Web client

Next.js client for the StreamHub VOD platform. It talks to the microservices only
through Kong (`/api/v1`) and contains no domain, catalog or persistence logic.

## Requirements

- Node.js 22 LTS
- npm 11

## Run locally

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:3001>. Port 3000 is used by Grafana in the root
`docker-compose.yml`.

By default the client uses the real services through Kong
(`NEXT_PUBLIC_USE_MOCKS=false`). Start the backend stack from the repository root
first. The browser calls same-origin `/api/v1`, and Next rewrites it to Kong, so
`http://localhost:3001` and the network URL both work. For an offline demo, set
`NEXT_PUBLIC_USE_MOCKS=true` and `NEXT_PUBLIC_MOCK_SCOPE=all`.

## Environment

Create `frontend/.env.local` from `.env.example`:

```dotenv
NEXT_PUBLIC_API_BASE_URL=/api/v1
KONG_INTERNAL_URL=http://localhost:8000
NEXT_PUBLIC_USE_MOCKS=false
NEXT_PUBLIC_MOCK_SCOPE=all
```

| Variable | Description |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | Same-origin `/api/v1` by default; Next rewrites it to Kong. Use an absolute URL only for direct Kong access |
| `KONG_INTERNAL_URL` | Server-side Kong address used by the rewrite (default `http://localhost:8000`) |
| `NEXT_PUBLIC_USE_MOCKS` | `false` sends every request to Kong. `true` starts the MSW worker |
| `NEXT_PUBLIC_MOCK_SCOPE` | Only used with mocks: `all` mocks every endpoint (offline/E2E) |

| Mode | Login / profiles | Home | Ping / like |
|---|---|---|---|
| Real (`USE_MOCKS=false`) | Kong → auth-service | Kong → catalog-service | Kong → telemetry-service |
| Offline (`USE_MOCKS=true`, `all`) | mock | mock | mock |

## Demo accounts (mocks and auth-service seed)

| Account | Password | Profiles |
|---|---|---|
| `ana@example.com` | `S3cret-pass` | Ana (adult), Sam (kids) |
| `luis@example.com` | `S3cret-pass` | Luis (adult) |

## Scripts

| Script | Purpose |
|---|---|
| `npm run dev` | Development server on port 3001 |
| `npm run build` | Production build |
| `npm run start` | Production server on port 3001 |
| `npm run lint` | ESLint |
| `npm run typecheck` | `tsc --noEmit` |
| `npm test` | Vitest unit tests |
| `npm run test:watch` | Vitest in watch mode |
| `npm run test:e2e` | Playwright with MSW on port 3101 (requires `npx playwright install chromium`) |
| `test:e2e` + `E2E_REAL=1` | Playwright against the real backend on port 3001 |
| `npm run format` | Prettier |

## What is implemented

- Login with the account token (`POST /auth/login`).
- Profile list and selection (`GET /profiles`, `POST /profiles/select`).
- Home screen from `GET /home`, including `categories`, `continue_watching`,
  `recommended` and the `source` badge.
- Title detail and a player preview.
- Playback telemetry every 10 seconds while playing, with stable `event_id`
  retries and a flush on pause/unmount.
- Like/Dislike with `202 Accepted` feedback.
- Standard error envelope with `correlation_id` shown in the UI.
- Route guards for account and profile tokens.
- Jaeger traces for the gateway and services (`Kong → telemetry-service →
  recommendation-worker` for a ping).

The real `catalog-service` currently returns title summaries (`content_id`,
`name`, `poster`). The UI tolerates missing `synopsis`, `duration_seconds`,
`age_rating` and `genres`; it shows the poster gradient and hides empty metadata
until a title-detail endpoint exists.

### E2E

The default suite runs with MSW on port 3101:

```bash
npm run test:e2e
```

The real-backend suite runs on port 3001 with `NEXT_PUBLIC_USE_MOCKS=false`.
With the Docker stack running and no dev server on port 3001:

```powershell
$env:E2E_REAL="1"; npm run test:e2e
```

```bash
E2E_REAL=1 npm run test:e2e
```

Next.js allows only one dev server per project directory, so stop your local
`npm run dev` before running the suite.

## Architecture boundary

```
Browser → Next.js Web App → Kong → microservices
                 (same-origin /api/v1 rewrite)
```

- The client never accesses a database, Redis, Cassandra or RabbitMQ.
- It is a separate deployable; it will get its own service in `docker-compose.yml`
  when deployment is added.
- Mocks live in `src/mocks/` and are disabled with `NEXT_PUBLIC_USE_MOCKS=false`.
- The source of truth for contracts is `docs/spec.md`.

## Folder structure

```
src/
  app/                 # Next.js routes
  features/
    auth/              # login, profiles, session, guards
    home/              # home screen, rails and lookups
    catalog/           # title detail
    playback/          # player preview
    interactions/      # like / dislike
    telemetry/         # playback pings
  shared/
    api/               # client, contracts, errors, config
    lib/               # formatting and ids
    ui/                # buttons, fields, posters, notices
  mocks/               # MSW handlers, fixtures and provider
```

## Integration with the real services

All services are wired: auth, catalog, telemetry and interactions are routed by
Kong, CORS is allowed for `http://localhost:3001`, and the Home response follows
`docs/spec.md` section 3.4. Keep `NEXT_PUBLIC_USE_MOCKS=false` (the default).

For the local stack:

1. Create the root `.env` from `.env.example` with `JWT_SECRET` and `MONGODB_URI`.
2. Start the services:
   ```bash
   docker compose up -d --build postgres auth-service mongo mongo-seed \
     redis redis-seed catalog-service telemetry-service \
     rabbitmq cassandra cassandra-init recommendation-worker \
     otel-collector jaeger kong
   ```
3. Keep `NEXT_PUBLIC_USE_MOCKS=false` in `frontend/.env.local`.
4. Open Jaeger at <http://localhost:16686> to inspect the traces.

The client also tolerates the raw Redis projection (`recommended` as ids,
`title_ids`) through `normalizeHome`, but the service returns the documented
object shape.
