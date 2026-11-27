# Code map

Module → key files → seam index for the label-follower monorepo, plus the app entry points. The two stack lanes carry their own detailed maps; this root map indexes them and covers the pieces that sit outside a lane. Vocabulary: **module**, **interface**, **seam**, **adapter**.

## Lanes

### `backend/` — Kotlin / Spring Boot service
**Purpose**: the HTTP API, the label/track store and the Spotify integration that discovers new tracks and builds playlists.
**Map**: [`backend/docs/code-map.md`](../backend/docs/code-map.md)
**Seam(s)**: the usecase ports (`OurInfoGateway`, `ExternalInfoGateway`, `UserInfoGateway`) separate business logic from persistence and Spotify; the lane map holds the per-package detail and the layering guard.

### `frontend/` — React / Vite SPA
**Purpose**: the browser screens — dashboard, label explorer, consolidation and the Spotify OAuth callback — over the backend's HTTP API.
**Map**: [`frontend/docs/code-map.md`](../frontend/docs/code-map.md)
**Seam(s)**: `api/` is the only HTTP boundary, `pages/` owns the routed screens, and `lib/isrc.ts` is the one ISRC validation; the lane map holds the per-module detail.

## Cross-cutting

### Wire contract — `frontend/src/api/types.ts` ↔ backend DTOs
**Purpose**: the one contract the two lanes share.
**Files**: `frontend/src/api/types.ts`, `backend/src/main/kotlin/com/rafaelfo/labelfollower/models/Track.kt`, `backend/src/main/kotlin/com/rafaelfo/labelfollower/api/*Controller.kt`
**Seam(s)**: **DTO mirror** — a request or response shape change edits both sides in the same commit; there is no codegen. The wire shape is camelCase except the snake_case health contract.

### Deploy — the combined image
**Purpose**: ship the backend jar and the built SPA together behind nginx.
**Files**: `Dockerfile`, `deploy/entrypoint.sh`, `deploy/nginx.conf.template`, `deploy/supervisord.conf`, `docker-compose.yml`
**Seam(s)**: **Migration before boot** (`deploy/entrypoint.sh`) runs the standalone migrator, then starts supervisord; **proxy seam** (`deploy/nginx.conf.template`) serves the SPA and strips `/api` before forwarding to the jar.

### Root tooling — shims and gates
**Purpose**: one command surface at the repo root and the checks CI and pre-commit run.
**Files**: `package.json`, `.pre-commit-config.yaml`, `scripts/release.sh`, `.github/workflows/ci.yml`, `.github/workflows/docker-publish.yml`, `.github/release.yml`, `CHANGELOG.md`, `docker-compose.yml`
**Seam(s)**: **script shims** (`package.json`) delegate to the lane commands (`be:*`, `fe:*`, `check`); **checks seam** (`.pre-commit-config.yaml`) scopes detekt, typecheck and the lane test commands by path; **release seam** (`docker-publish.yml` + `scripts/release.sh`) builds the image with `push: false` on pull requests and publishes immutable semver/short-SHA tags, moving `latest` only on a `v*` release tag — never on a merge.

## Entry points

- `backend/src/main/kotlin/com/rafaelfo/labelfollower/application/Boot.kt` — Spring Boot `main()`; component-scans the whole package.
- `backend/src/main/kotlin/com/rafaelfo/labelfollower/config/migration/Migrator.kt` — standalone `main()`; applies the Flyway migrations, run by `deploy/entrypoint.sh`.
- `frontend/src/main.tsx` — mounts React with the QueryClient and the browser router.
- `frontend/src/App.tsx` — the route table (`/`, `/introspect`, `/consolidate`, `/callback`).
- `deploy/entrypoint.sh` — container boot: migrate, render the nginx config, start supervisord.
- `deploy/supervisord.conf` — runs the API jar and nginx in one image.
- `docker-compose.yml` — the local full stack (app image + MySQL) and the host port block.
