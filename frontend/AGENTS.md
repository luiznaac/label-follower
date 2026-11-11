# AGENTS.md — label-follower frontend

React 19 + Vite + TypeScript + Tailwind v4 + TanStack Query + React Router SPA — same stack and conventions across all of luiznaac's frontends, see salgadinhos' `react-spa-screen` skill for the shared parts (Tailwind v4-in-CSS, TanStack Query v5 object syntax, the `api/`→`components/`→`pages/`→`lib/` layout, the dev proxy/base-path setup). This file only covers what's specific to label-follower.

## Code map

- [`docs/code-map.md`](docs/code-map.md)

## Commands

```bash
npm run dev         # vite dev server, http://127.0.0.1:8123 (see "Spotify auth" below)
npm run typecheck    # tsc -b --noEmit
npm run check         # typecheck + Biome + Vitest — run this before opening a PR
npm run build         # tsc -b && vite build
```

### Fresh worktree setup (sandbox)

A fresh worktree has no `node_modules`, so the first `npm test` fails with `vitest: not found`. From the monorepo root (this lane's parent), install the lane's dependencies inside the salgadinhos sandbox:

```bash
node <salgadinhos>/adapters/sandbox/run.mjs --stack node --workspace <repo-root> -- npm --prefix frontend ci
```

`--stack node` is required because the monorepo root's `package.json` is a dependency-less scripts shim, so the runner's marker detection intentionally does not detect it (its error suggests pointing at `frontend/`). The command mirrors the frontend install in `npm run setup`, but the install runs in the sandbox, never on the host — the rule and setup live in `salgadinhos` at `global/AGENTS.md` → "Sandbox for third-party installs" and `docs/salgadinhos/sandbox-bootstrap.md`.

## Spotify auth starts in the browser and finishes on the backend

`POST /consolidate` uses a Spotify login the backend holds. `src/lib/spotifyAuth.ts` only sends the browser to Spotify's consent screen; `pages/SpotifyCallback.tsx` receives the `code` and posts it to `POST /auth/spotify/exchange`, where the backend trades it for a permanent login. The browser never stores a token. The dev server binds `127.0.0.1` specifically (not `localhost`) because Spotify's redirect-URI matching is exact and rejects `http://localhost` — don't "simplify" this to `localhost`.

## Label-follower-specific pieces

- **`api/types.ts` mirrors `Track`/`Label`** (`backend/.../models/`) and the JSON shapes `api/` controllers return — smaller surface than the sibling repos, no codegen either. Update it in the same commit as any backend DTO change.
- **Port 8123** (`FRONTEND_DEV_PORT`, part of label-follower's port block — see the [root README's table](../README.md#ports)) with `strictPort: true`, so a taken port fails loudly instead of auto-incrementing: the served origin must stay identical to the Spotify redirect registered against it. `vite.config.ts` reads the env files through Vite's `loadEnv` — the config runs in Node and sees `process.env`, but Vite does not put the `.env`-file values there; the proxy target is `VITE_API_TARGET ?? http://localhost:8120`.
- **`components/IsrcInput.tsx`/`TrackList.tsx`** are the core consolidation-flow UI; `SpotifyConnectButton.tsx` kicks off the PKCE flow.

## Coding standards

- [`CODING_STANDARDS.md`](CODING_STANDARDS.md) — "the language-idiom rules the tooling does not enforce"

Git/PR conventions: see `salgadinhos/global/AGENTS.md`.
