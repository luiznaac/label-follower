# Code map

Module → key files → seam index for the `frontend/` lane, plus the app entry points. React 19 + Vite + TypeScript, Tailwind v4-in-CSS, TanStack Query v5 and React Router; `api/` → `components/` → `pages/` → `lib/`. Vocabulary: **module**, **interface**, **seam**, **adapter**.

## Modules

### `src/api` — backend adapter and wire types
**Purpose**: the only place that talks HTTP — the fetch wrapper, the hand-mirrored backend DTOs and the TanStack Query hooks.
**Files**: `frontend/src/api/client.ts`, `frontend/src/api/types.ts`, `frontend/src/api/queries.ts`
**Seam(s)**:
- **DTO mirror** (`api/types.ts`) — mirrors `backend/.../models/Track.kt` and the JSON returned by `backend/.../api/*Controller.kt` (camelCase, no `@JsonProperty`); one alias per endpoint (`TrackLookupResponse`, `LabelCatalogResponse`, `DiscoverNewTracksResponse`, `ConsolidateResponse`, `SpotifyStatusResponse`, `ExchangeCodeRequest`). A backend DTO change edits this file in the same commit.
- **Fetch adapter** (`api/client.ts`) — `BASE` is `VITE_API_BASE ?? "/api"`; `ApiError` carries `status` and the detail from Spring's `{message,error}` body; `request<T>` guards `204` and empty bodies (the `/consolidate` endpoint returns no body).
- **Query seam** (`api/queries.ts`) — the `keys` factory (`track`, `labelCatalog`, `spotifyStatus`) is the cache contract; `useTrack`/`useLabelCatalog` normalize and gate the ISRC before firing, `useDiscoverNewTracks` invalidates the label catalogue, and the Spotify status/exchange/disconnect mutations invalidate `keys.spotifyStatus`.

### `src/pages` — routed screens
**Purpose**: the four screens — dashboard, label explorer, consolidation and the OAuth callback.
**Files**: `frontend/src/pages/Dashboard.tsx`, `pages/Explorer.tsx`, `pages/Consolidate.tsx`, `pages/SpotifyCallback.tsx`
**Seam(s)**:
- **Explorer** (`pages/Explorer.tsx`) — the ISRC in the URL search params drives three queries (track, catalogue, new-track discovery); the discover mutation is reset when the ISRC changes.
- **Consolidate** (`pages/Consolidate.tsx`) — the Spotify connection gate: `useSpotifyStatus()` chooses connect vs the consolidate/disconnect controls.
- **OAuth callback** (`pages/SpotifyCallback.tsx`) — exchanges the `code` once (`useRef` guard against React StrictMode's double effect) and navigates to `/consolidate`; the backend stores the token.

### `src/components` — chrome and consolidation widgets
**Purpose**: the layout chrome plus the ISRC input, track list, status tiles and Spotify connect button.
**Files**: `frontend/src/components/{Layout,IsrcInput,TrackList,StatTile,SpotifyConnectButton}.tsx`
**Seam(s)**:
- **Chrome seam** (`components/Layout.tsx`) — the header nav (`/`, `/introspect`, `/consolidate`) and the routed `<Outlet />`.
- **ISRC gate** (`components/IsrcInput.tsx`) — normalizes and validates through `lib/isrc.ts`, disables submit while invalid and handles Enter explicitly; every ISRC entry point goes through it.
- **Track rendering** (`components/TrackList.tsx`) — one row per `Track`, keyed by ISRC/Spotify id; `StatTile` is the dashboard tile primitive.

### `src/lib` — pure helpers and the Spotify redirect
**Purpose**: the unit-tested ISRC helpers plus the client-side start of the Spotify authorization flow.
**Files**: `frontend/src/lib/isrc.ts`, `lib/isrc.test.ts`, `lib/spotifyAuth.ts`
**Seam(s)**:
- **ISRC rule** (`lib/isrc.ts`) — `ISRC_RE` (`^[A-Z]{2}[A-Z0-9]{3}\d{7}$`), `normalizeIsrc` (strip spaces/hyphens, uppercase) and `isValidIsrc`; the single validation used by both the input and the query hooks, pinned by `isrc.test.ts`.
- **Spotify redirect** (`lib/spotifyAuth.ts`) — `SPOTIFY_REDIRECT_URI` (exact-match loopback) and `beginSpotifyLogin()` build the consent URL; the returned code is exchanged **server-side** through `useExchangeSpotifyCode`. No token is stored in the browser.

### Theme and root wiring
**Purpose**: the design tokens and the Vite/TypeScript/Biome build.
**Files**: `frontend/src/index.css`, `frontend/vite.config.ts`, `frontend/index.html`, `frontend/package.json`, `frontend/tsconfig*.json`, `frontend/biome.json`, `frontend/vitest.config.ts`
**Seam(s)**:
- **Theme seam** (`src/index.css`) — the `@theme` block owns the Spotify-green `brand-*` ramp and the `surface-*` background scale; the components consume those tokens by name.
- **Base-path seam** (`vite.config.ts`) — `base` is `/label-follower/` in production and `/` in dev; `main.tsx` derives the router `basename` from `import.meta.env.BASE_URL`.
- **Loopback seam** (`vite.config.ts`) — binds `127.0.0.1` (not `localhost`) on port 5274 with `strictPort`, because Spotify's redirect-URI matching is exact; the `/api` proxy targets `VITE_API_TARGET ?? http://localhost:8080`.
- **Quality gate** (`package.json`) — `npm run check` = `typecheck` + Biome + Vitest; `npm test` runs Vitest.

## Entry points

- `frontend/src/main.tsx` — mounts React `StrictMode` with the `QueryClient` and the browser router.
- `frontend/src/App.tsx` — the route table: `/` under `Layout` (index `Dashboard`, `introspect`, `consolidate`) plus `/callback` outside the chrome.
- `frontend/index.html` — the Vite HTML shell with the `#root` mount.
- `frontend/vite.config.ts` — dev server host/port, base path and API proxy.
