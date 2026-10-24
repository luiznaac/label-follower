# AGENTS.md — label-follower frontend

React 19 + Vite + TypeScript + Tailwind v4 + TanStack Query + React Router SPA — same stack and conventions across all of luiznaac's frontends, see salgadinhos' `react-spa-screen` skill for the shared parts (Tailwind v4-in-CSS, TanStack Query v5 object syntax, the `api/`→`components/`→`pages/`→`lib/` layout, the dev proxy/base-path setup). This file only covers what's specific to label-follower.

## Code map

- [`docs/code-map.md`](docs/code-map.md)

## Commands

```bash
npm run dev         # vite dev server, http://127.0.0.1:5274 (see "Spotify auth" below)
npm run typecheck    # tsc -b --noEmit
npm run check         # typecheck + Biome + Vitest — run this before opening a PR
npm run build         # tsc -b && vite build
```

## Spotify auth starts in the browser and finishes on the backend

`POST /consolidate` uses a Spotify login the backend holds. `src/lib/spotifyAuth.ts` only sends the browser to Spotify's consent screen; `pages/SpotifyCallback.tsx` receives the `code` and posts it to `POST /auth/spotify/exchange`, where the backend trades it for a permanent login. The browser never stores a token. The dev server binds `127.0.0.1` specifically (not `localhost`) because Spotify's redirect-URI matching is exact and rejects `http://localhost` — don't "simplify" this to `localhost`.

## Label-follower-specific pieces

- **`api/types.ts` mirrors `Track`/`Label`** (`backend/.../models/`) and the JSON shapes `api/` controllers return — smaller surface than the sibling repos, no codegen either. Update it in the same commit as any backend DTO change.
- **Port 5274**, not 5273 (shougong's default) — chosen so both dev servers can run at once. Keep them distinct if you copy this scaffold's `vite.config.ts` pattern elsewhere.
- **`components/IsrcInput.tsx`/`TrackList.tsx`** are the core consolidation-flow UI; `SpotifyConnectButton.tsx` kicks off the PKCE flow.

Git/PR conventions: see `salgadinhos/global/AGENTS.md`.
