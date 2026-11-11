# label-follower-fe

The frontend of [`label-follower`](../backend). A React + Vite + TypeScript + Tailwind v4 SPA over the backend's HTTP API. Same stack and conventions as [`shougong/frontend`](../../shougong/frontend).

A dark, sober look with a Spotify-green accent.

## Prerequisites

- **Node.js 20+**.
- The backend running: `cd backend && SPOTIFY_CLIENT_SECRET=… ./gradlew bootRun` (→ `http://localhost:8120`).
- For the **Consolidate** screen: register `http://127.0.0.1:8123/callback` as a _Redirect URI_ in the Spotify app (client id `5a5af3ea4d104213872ebff79136fcda`). Spotify rejects `http://localhost` — it has to be the literal IP.

## Run it in dev

```bash
npm install
npm run dev
```

Opens at `http://127.0.0.1:8123` (`FRONTEND_DEV_PORT`, part of the project's port block — see the [root README's table](../README.md#ports)). Calls to `/api/*` are _proxied_ to `http://localhost:8120` with the `/api` prefix stripped (configured in `vite.config.ts`, adjustable through `VITE_API_TARGET`). That keeps the backend free of CORS config.

> On PowerShell, if `npm` is blocked by the _execution policy_ (`npm.ps1 cannot be loaded`), use `npm.cmd install` / `npm.cmd run dev`, or allow it with `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

## Build

```bash
npm run build      # emits dist/ with the /label-follower/ base path (for the reverse proxy)
npm run preview
```

To build at the root (`/`) instead of `/label-follower/`: `VITE_BASE=/ npm run build` (that is what the `Dockerfile` does).

## Screens

| Route          | What                                                                     |
| -------------- | ------------------------------------------------------------------------ |
| `/`            | Dashboard: Spotify connection status and shortcuts to the other screens  |
| `/introspect`  | Look a track up by ISRC, show the label's recent catalogue and discover new tracks |
| `/consolidate` | Connect Spotify and fire `POST /consolidate` for every followed label     |
| `/callback`    | The Spotify OAuth redirect target — exchanges the `code` and returns to `/consolidate` |

## Architecture notes

- `src/api/` — the typed HTTP client (`client.ts`) plus the TanStack Query hooks (`queries.ts`). The types in `types.ts` **mirror** `models/Track.kt` and the backend controllers' responses; any change on either side lands in the same commit (there is no codegen).
- `src/lib/spotifyAuth.ts` — the Spotify user token for `POST /consolidate` is obtained **entirely in the browser** through Authorization Code + PKCE (`accounts.spotify.com`, with CORS open to public PKCE clients). The backend keeps using client-credentials for its own reads. The token (access + refresh) lives in `localStorage`.
- `src/lib/isrc.ts` — normalizes (uppercase, no hyphens) and validates ISRCs; queries only fire for a valid ISRC.
- `POST /consolidate` responds `200` with no body — the screen only shows started / finished / error. Playlist creation uses a fixed Spotify user on the backend (`SpotifyUserPlaylistGateway`), so in practice only that account can consolidate.
