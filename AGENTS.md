# AGENTS.md — label-follower monorepo

Development guidelines for anyone (human, agent, or tool) working in this repository.

Surfaces: **HTTP + Browser**. The backend serves the HTTP API; the SPA is the browser surface. Boot and check commands live in [README.md](README.md); the app entry points live in [docs/code-map.md](docs/code-map.md).

Two projects, one repo (layout borrowed from [shougong](https://github.com/luiznaac/shougong/blob/master/AGENTS.md)):

- **`backend/`** — the Kotlin / Spring Boot service (plain Spring MVC, single Gradle module). Backend tasks run through the Gradle wrapper in `backend/`. Architecture, conventions and rules for evolving it are in [backend/AGENTS.md](backend/AGENTS.md) — read that before touching `backend/`. It is deliberately *not* the multi-module Ktor shape of the other repos in this family; don't "upgrade" it.
- **`frontend/`** — the React / Vite SPA. Frontend scripts run through the root shims (`npm run fe:*`). Details in [frontend/AGENTS.md](frontend/AGENTS.md).

## Code map

- [`docs/code-map.md`](docs/code-map.md)

## Cross-cutting rule

`frontend/src/api/types.ts` is a hand-maintained mirror of the backend DTOs — today just `backend/src/main/kotlin/com/rafaelfo/labelfollower/models/Track.kt` and the JSON shapes the controllers in `backend/.../api/` return. Any change to a request/response shape on one side must update the other in the same commit. There is no codegen.

## Conventions

- The SPA always calls `/api/*`; the Vite dev server proxies it to the backend and strips the prefix. Same-origin from the browser, so the backend has no CORS config — keep it that way.
- Spotify user auth for `POST /consolidate` starts in the browser (Authorization Code + PKCE); the backend exchanges the code and holds the login. The Spotify app needs the dev redirect URI registered — see the [Spotify app runbook](docs/runbooks/spotify-app.md).

## Config & secrets

Env vars and their dev fixtures: [`.env.example`](.env.example) is the reference; the gitignored `.env` supplies them locally and is loaded into the backend run. There is no `dev-*` fixture — Spotify rejects a fake client secret. The single secrets doc is `docs/salgadinhos/secrets.md` in the salgadinhos repo.

## Documentation

- [Family documentation standard](https://github.com/luiznaac/salgadinhos/blob/master/docs/salgadinhos/documentation-standard.md) — the README and doc-set contract this repo follows.
- [refresh-docs skill](https://github.com/luiznaac/salgadinhos/blob/master/skills/refresh-docs/SKILL.md) — run its audit when a change hits the standard's change-kind matrix.
