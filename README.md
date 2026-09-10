# label-follower

Keep up with the record labels you follow on Spotify: label-follower finds new tracks they've
released and builds a Spotify playlist out of them automatically, instead of you checking each
label's page by hand.

Two projects in one repo:

| Directory | What it is |
|---|---|
| [`backend/`](backend/) | Kotlin + Spring Boot service — the label/track discovery and playlist logic. Plain Spring MVC, single Gradle module. |
| [`frontend/`](frontend/) | React + Vite SPA — a small web UI over the backend's API. |

The frontend's `frontend/src/api/types.ts` mirrors the backend's `Track` model (and the JSON its
controllers return); keep them in sync in the same change — that's the reason these two live in
one repo. Same monorepo shape and frontend stack as the sibling
[shougong](../shougong/README.md).

## Quick start

```bash
# API — http://localhost:8080
cd backend && export SPOTIFY_CLIENT_SECRET=your-spotify-app-client-secret && ./gradlew bootRun

# SPA — http://127.0.0.1:5274 (proxies /api -> :8080), in another terminal
npm --prefix frontend install
npm --prefix frontend run dev
```

You need JDK 17, Node 20+, and a Spotify API app (`SPOTIFY_CLIENT_SECRET`; the client ID is public
and already in `backend/src/main/resources/application.properties`). For the "Consolidate" page,
register `http://127.0.0.1:5274/callback` as a redirect URI in the Spotify app.

See [backend/README.md](backend/README.md) and [frontend/README.md](frontend/README.md) for
details, and [DEVELOPMENT.md](DEVELOPMENT.md) for architecture and conventions.

## Docker

```bash
docker compose up --build   # single image: nginx (SPA + /api proxy) + the backend jar -> http://localhost:8081
```

## Note

This is the oldest project in this author's collection of personal services — the `backend/` half
is simpler and structured a bit differently (plain Spring MVC, single module) than its siblings
([chameidor](../chameidor/README.md), [portfolio-2](../portfolio-2/README.md)), which use a
Ktor-based multi-module architecture.
