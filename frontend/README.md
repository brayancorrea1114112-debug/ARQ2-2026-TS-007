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

The backend is not required in `all` mock mode. To use the real `auth-service`
and `catalog-service`, start the stack from the repository root with
`docker compose up` (Kong is exposed on `http://localhost:8000`) and keep
`NEXT_PUBLIC_MOCK_SCOPE=missing`, which only mocks the services that do not exist
yet.

## Environment

Create `frontend/.env.local` from `.env.example`:

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://localhost:8000/api/v1
NEXT_PUBLIC_USE_MOCKS=true
NEXT_PUBLIC_MOCK_SCOPE=missing
```

| Variable | Description |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | Kong proxy base URL. Always ends at `/api/v1` |
| `NEXT_PUBLIC_USE_MOCKS` | Starts the MSW worker. `false` sends everything to Kong |
| `NEXT_PUBLIC_MOCK_SCOPE` | `all`: mock every endpoint (offline/E2E). `missing`: mock only telemetry and interactions |

| Mode | Login / profiles | Home | Ping / like |
|---|---|---|---|
| `all` | mock | mock | mock |
| `missing` | Kong → auth-service | Kong → catalog-service | mock |
| `USE_MOCKS=false` | Kong → auth-service | Kong → catalog-service | Kong (fails until the Telemetry Service exists) |

## Demo accounts (mocks)

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

The real `catalog-service` currently returns title summaries (`content_id`,
`name`, `poster`). The UI tolerates missing `synopsis`, `duration_seconds`,
`age_rating` and `genres`; it shows the poster gradient and hides empty metadata
until a title-detail endpoint exists.

### E2E against the real backend

With the Docker stack running and no dev server on port 3001:

```powershell
$env:E2E_REAL="1"; npm run test:e2e
```

```bash
E2E_REAL=1 npm run test:e2e
```

Next.js allows only one dev server per project directory, so stop your local
`npm run dev` before running the E2E suite.

## Architecture boundary

```
Browser → Next.js Web App → Kong → microservices
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

The Catalog & Home Service is already wired: `GET /api/v1/home` is routed by Kong
and the browser is allowed by the Kong `cors` plugin. Use
`NEXT_PUBLIC_MOCK_SCOPE=missing` to connect auth and home to the real backend.

For the local stack:

1. Create the root `.env` from `.env.example` with `JWT_SECRET` and `MONGODB_URI`.
2. `docker compose up -d postgres auth-service mongo mongo-seed redis redis-seed catalog-service kong`.
3. Keep `NEXT_PUBLIC_MOCK_SCOPE=missing` in `frontend/.env.local`.

When the Telemetry Service exists:

1. Add `telemetry` and `interactions` routes to `kong/kong.yml`.
2. Set `NEXT_PUBLIC_USE_MOCKS=false` to remove the last mocks.
3. Keep the Home response shape from `docs/spec.md` section 3.4. The client also
   tolerates the raw Redis projection (`recommended` as ids, `title_ids`) through
   `normalizeHome`, but the service should return the documented object shape.
