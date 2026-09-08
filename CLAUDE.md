# CLAUDE.md — label-follower monorepo

Two projects, one repo (layout borrowed from [shougong](../shougong/CLAUDE.md)):

- **`backend/`** — the Kotlin / Spring Boot service (plain Spring MVC, single Gradle module). All
  backend commands run from `backend/` (`cd backend && ./gradlew <task>`). Its architecture,
  conventions and the rules for evolving it are in [backend/CLAUDE.md](backend/CLAUDE.md) — read
  that before touching `backend/`. It is deliberately *not* the multi-module Ktor shape of the
  other repos in this family; don't "upgrade" it.
- **`frontend/`** — the React / Vite SPA. Commands run from `frontend/`
  (`npm --prefix frontend run <script>`). Details in [frontend/README.md](frontend/README.md).

## The one cross-cutting rule

`frontend/src/api/types.ts` is a hand-maintained mirror of the backend DTOs — today just
`backend/src/main/kotlin/com/rafaelfo/labelfollower/models/Track.kt` and the JSON shapes the
controllers in `backend/.../api/` return. Any change to a request/response shape on one side must
update the other in the same commit. There is no codegen.

## Tooling

Root `package.json` holds script shims only (`npm run be:run`, `npm run be:check`,
`npm run fe:dev`, `npm run fe:build`, `npm run check`, `npm run up`). It has no dependencies and is
not a real package. `.pre-commit-config.yaml` lives at the root and scopes hooks by path
(`^backend/.*\.kt$` → detekt, `^frontend/.*\.(ts|tsx)$` → `tsc` typecheck).

`.github/workflows/ci.yml` runs two independent jobs — `backend` (`./gradlew detekt test`) and
`frontend` (`npm ci && npm run typecheck && npm run build`).

## Dev

Two processes:

```bash
cd backend && SPOTIFY_CLIENT_SECRET=… ./gradlew bootRun   # API  -> http://localhost:8080
npm --prefix frontend run dev                              # SPA  -> http://127.0.0.1:5274
```

The SPA always calls `/api/*`; the Vite dev server proxies that to the backend and strips the
`/api` prefix (`frontend/vite.config.ts`). Same-origin from the browser's point of view, so the
backend needs no CORS config — keep it that way.

Spotify user auth for `POST /consolidate` happens entirely in the browser (Authorization Code +
PKCE, `frontend/src/lib/spotifyAuth.ts`). The backend still only does client-credentials for its
own reads. The Spotify app needs the dev redirect URI `http://127.0.0.1:5274/callback` registered
(Spotify rejects `http://localhost`).

## Docker

One image (repo-root `Dockerfile`, multi-stage) ships backend + frontend together: `supervisord`
runs the Spring Boot jar (`API_PORT`/8080) and `nginx` (`deploy/nginx.conf.template` — serves the
built SPA on `WEB_PORT`/8081 and reverse-proxies `/api` → the jar). `docker-compose.yml` at the
root wraps it for local runs (mount a volume for
`/app/tooLazyToImplementPersistenceRightNow` — the backend's flat-file persistence).
`.github/workflows/docker-publish.yml` pushes `luiznaac/label-follower:latest` + `:sha-<short>` on
master pushes that touch `backend/`, `frontend/`, `Dockerfile`, or `deploy/`.
