# label-follower — backend

The Kotlin/Spring service. It helps you keep up with the record labels you follow on Spotify: it
finds new tracks they've released and can build a Spotify playlist out of them automatically,
instead of you checking each label's page by hand.

This is one half of a two-project repo — see the root [README](../README.md) and the
[frontend](../frontend/README.md).

## What it actually does for you

1. **Start from a track you like** (identified by its ISRC code) — label-follower can figure out
   which label released it.
2. **"Introspect" a label** — look at everything that label has released.
3. **Find what's new** — compare the label's releases against what's already known/stored, and
   surface only the tracks that are genuinely new.
4. **Consolidate across all followed labels at once** — for every label with new tracks, create a
   Spotify playlist with them on the Spotify account connected to the backend (one playlist per
   label per run).

## Using it

### Requirements

- JDK 17.
- MySQL — `docker compose -f backend/docker-compose.yml up -d mysql` (or the root compose file for the full stack).
- A Spotify API app — you need a `SPOTIFY_CLIENT_SECRET` environment variable set to run it (the
  client ID is already in `application.properties` and isn't secret).

### Run it locally

```bash
cd backend
export SPOTIFY_CLIENT_SECRET=your-spotify-app-client-secret
./gradlew bootRun
```

Serves on `http://localhost:8080`.

### What you can do through the API

- `GET /track/{isrc}` — look up a track by its ISRC.
- `GET /introspect/fromTrack/{isrc}` — find which label released a given track (its recent catalog).
- `POST /introspect/newTracks/{isrc}` — find new tracks from the label that released a given track.
- `POST /consolidate` — go through every followed label, find new tracks for each, and create one
  playlist per label with new tracks on the connected Spotify account. Needs an account connected
  first (see below); takes no token or body.
- `GET /auth/spotify/status` — `{ "connected": boolean }`: whether the backend holds a Spotify login.
- `POST /auth/spotify/exchange` — `{ "code", "redirectUri" }`: trades the OAuth authorization code
  (from the frontend's `/callback`) for a refresh token the backend stores in MySQL.
- `DELETE /auth/spotify` — forgets the stored Spotify login.

Spotify auth is two-fold, both held by the backend: client credentials (`SpotifyAuth`) for catalogue
reads, and a single user login (`SpotifyUserAuth`, refresh token in the `spotify_account` table) for
creating playlists. The frontend only redirects the browser to Spotify's consent screen.

Full API reference, flows and business rules (in Portuguese): [docs/tecnico.md](../docs/tecnico.md),
[docs/negocio.md](../docs/negocio.md). Known bugs and the improvement plan:
[docs/bugs-e-melhorias.md](../docs/bugs-e-melhorias.md).

### Build & test

```bash
cd backend
./gradlew build
./gradlew test
./gradlew detekt
```

## Where things live

- `src/main/kotlin/com/rafaelfo/labelfollower/api/` — the HTTP endpoints listed above.
- `.../usecases/` — the actual "find new tracks / build a playlist" logic.
- `.../integrations/spotify/` — talks to the real Spotify API.
- `.../integrations/database/` — where label/track info we already know about is stored (MySQL, via Exposed).
- `.../models/` — the core `Label` and `Track` concepts.

See [DEVELOPMENT.md](DEVELOPMENT.md) for architecture details and conventions if you're making changes.

## Note

This is the oldest project in this author's collection of personal services — the `backend/` module
is simpler and structured a bit differently (plain Spring MVC, single module) than its siblings
([chameidor](../../chameidor/README.md), [portfolio-2](../../portfolio-2/README.md)), which use a
Ktor-based multi-module architecture.
