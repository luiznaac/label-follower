# label-follower

Keep up with the record labels you follow on Spotify: label-follower finds new tracks they've released and builds a Spotify playlist out of them automatically, instead of you checking each label's page by hand.

Two projects in one repo:

| Directory | What it is |
|---|---|
| [`backend/`](backend/) | Kotlin + Spring Boot service — the label/track discovery and playlist logic. Plain Spring MVC, single Gradle module. |
| [`frontend/`](frontend/) | React + Vite SPA — a small web UI over the backend's API. |

The frontend's `frontend/src/api/types.ts` mirrors the backend's `Track` model (and the JSON its controllers return); keep them in sync in the same change — that's the reason these two live in one repo. Same monorepo shape and frontend stack as the sibling [shougong](../shougong/README.md).

## Quick start

```bash
# DB — localhost:8122
docker compose -f backend/docker-compose.yml up -d mysql

# API — http://localhost:8120
cd backend && export SPOTIFY_CLIENT_SECRET=your-spotify-app-client-secret && ./gradlew bootRun

# SPA — http://127.0.0.1:8123 (proxies /api -> :8120), in another terminal
npm --prefix frontend install
npm --prefix frontend run dev
```

You need JDK 17, Node 20+, and a Spotify API app (`SPOTIFY_CLIENT_SECRET`; the client ID is public and already in `backend/src/main/resources/application.properties`). For the "Consolidate" page, register `http://127.0.0.1:8123/callback` as a redirect URI in the Spotify app.

### Ports

label-follower owns one 10-port block in the family's local-dev allocation; these four are the roles it uses today (the committed record is [`.salgadinhos/ports.json`](.salgadinhos/ports.json)).

| Role | Host port | What |
|---|---|---|
| `api` | 8120 | Spring Boot backend — `http://localhost:8120` |
| `web` | 8121 | combined image: nginx serving the SPA + `/api` proxy — `http://localhost:8121` |
| `mysql` | 8122 | MySQL, host side (`docker compose port mysql 3306` reports it) |
| `frontend-dev` | 8123 | Vite dev server — `http://127.0.0.1:8123` |

Container-internal ports stay fixed (API `8080`, web `8081`, MySQL `3306`); only the host/published side uses the block. A fresh clone with no `.env` boots on it; a gitignored root `.env` (`KTOR_PORT`, `MYSQL_PORT`, plus compose-only `WEB_HOST_PORT`) or `frontend/.env.development.local` (`FRONTEND_DEV_PORT`, `VITE_API_TARGET`) can override locally.

See [backend/README.md](backend/README.md) and [frontend/README.md](frontend/README.md) for details, and [AGENTS.md](AGENTS.md) for architecture and conventions.

## Docker

```bash
docker compose up --build   # single image: nginx (SPA + /api proxy) + the backend jar -> http://localhost:8121
```

## Note

This is the oldest project in this author's collection of personal services — the `backend/` half is simpler and structured a bit differently (plain Spring MVC, single module) than its siblings ([chameidor](../chameidor/README.md), [portfolio-2](../portfolio-2/README.md)), which use a Ktor-based multi-module architecture.
