# DEVELOPMENT.md — label-follower monorepo

Development guidelines for anyone (human, agent, or tool) working in this repository.

Two projects, one repo (layout borrowed from [shougong](../shougong/DEVELOPMENT.md)):

- **`backend/`** — the Kotlin / Spring Boot service (plain Spring MVC, single Gradle module). All
  backend commands run from `backend/` (`cd backend && ./gradlew <task>`). Architecture,
  conventions and rules for evolving it are in [backend/DEVELOPMENT.md](backend/DEVELOPMENT.md) —
  read that before touching `backend/`. It is deliberately *not* the multi-module Ktor shape of
  the other repos in this family; don't "upgrade" it.
- **`frontend/`** — the React / Vite SPA. Commands run from `frontend/`
  (`npm --prefix frontend run <script>`). Details in [frontend/README.md](frontend/README.md).

## Cross-cutting rule

`frontend/src/api/types.ts` is a hand-maintained mirror of the backend DTOs — today just
`backend/src/main/kotlin/com/rafaelfo/labelfollower/models/Track.kt` and the JSON shapes the
controllers in `backend/.../api/` return. Any change to a request/response shape on one side must
update the other in the same commit. There is no codegen.

## Git workflow

**Do not commit directly to `master`.** Always create a feature branch and open a PR,
even for a small or "obviously safe" change. This applies to all contributors.

## Tooling

Root `package.json` holds script shims only (`npm run be:run`, `npm run be:check`,
`npm run db:migrate`, `npm run db:generate -- -Pname=V2__x`, `npm run fe:dev`, `npm run fe:build`,
`npm run check`, `npm run up`). It has no dependencies and is
not a real package. `.pre-commit-config.yaml` lives at the root and scopes hooks by path
(`^backend/.*\.kt$` → detekt, `^frontend/.*\.(ts|tsx)$` → `tsc` typecheck).

`.github/workflows/ci.yml` runs two independent jobs — `backend` (`./gradlew detekt test`) and
`frontend` (`npm ci && npm run typecheck && npm run build`).

## Dev

Three processes (the backend needs MySQL up first):

```bash
docker compose -f backend/docker-compose.yml up -d mysql   # DB   -> localhost:3306
cd backend && SPOTIFY_CLIENT_SECRET=… ./gradlew bootRun     # API  -> http://localhost:8080
npm --prefix frontend run dev                                # SPA  -> http://127.0.0.1:5274
```

The backend reads `MYSQL_HOST`/`MYSQL_USER`/`MYSQL_PASSWORD`; the defaults in
`application.properties` already match the compose command above (localhost, root, no
password — see the "Configuration" section of [backend/DEVELOPMENT.md](backend/DEVELOPMENT.md)). A
root `.env` (gitignored, see `.env.example`) is loaded into `./gradlew bootRun` automatically, so
secrets do not have to be exported by hand.

The SPA always calls `/api/*`; the Vite dev server proxies that to the backend and strips the
`/api` prefix (`frontend/vite.config.ts`). Same-origin from the browser's point of view, so the
backend needs no CORS config — keep it that way.

Spotify user auth for `POST /consolidate` is held by the backend: the SPA only redirects to
Spotify's consent screen (`frontend/src/lib/spotifyAuth.ts`), the `/callback` page posts the
authorization code to `POST /auth/spotify/exchange`, and the backend (`SpotifyUserAuth`) stores the
refresh token in MySQL. No token lives in the browser. Catalogue reads use the app's own
client-credentials token (`SpotifyAuth`). The Spotify app needs the dev redirect URI
`http://127.0.0.1:5274/callback` registered (Spotify rejects `http://localhost`).

## Documentation

`docs/` (in Portuguese) holds the business view ([docs/negocio.md](docs/negocio.md) — flows and
business rules RN-xx), the technical reference ([docs/tecnico.md](docs/tecnico.md) — API, sequence
diagrams, data model, config, deploy) and the known bugs / improvement plan
([docs/bugs-e-melhorias.md](docs/bugs-e-melhorias.md)). Keep them in step with behaviour changes:
a change to a business rule or an endpoint updates the matching section in the same PR.

## Docker

One image (repo-root `Dockerfile`, multi-stage) ships backend + frontend together: `supervisord`
runs the Spring Boot jar (`API_PORT`/8080) and `nginx` (`deploy/nginx.conf.template` — serves the
built SPA on `WEB_PORT`/8081 and reverse-proxies `/api` → the jar). `docker-compose.yml` at the
root wraps the full stack — the app image plus its own MySQL — for local runs
(`backend/docker-compose.yml` on its own runs just the DB, for a locally-run `./gradlew bootRun`).
The schema comes from `backend/src/main/resources/db/migration/V*.sql`, applied by Flyway from
`deploy/entrypoint.sh` before the app starts — see the "Database migrations" section of
[backend/DEVELOPMENT.md](backend/DEVELOPMENT.md).
`.github/workflows/docker-publish.yml` pushes `luiznaac/label-follower:latest` +
`:v<run-number>` (a sequential build number, `github.run_number`) on master pushes that touch
`backend/`, `frontend/`, `Dockerfile`, or `deploy/`.
