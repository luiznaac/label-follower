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
4. **Consolidate across all followed labels at once** and build a Spotify playlist containing all
   the newly discovered tracks, using the requesting user's Spotify account.

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
- `POST /consolidate` (requires a Spotify `Authorization: Bearer <token>` header) — go through
  every followed label, find new tracks for each, and create a playlist with all of them on the
  authenticated user's Spotify account.

The `Bearer` token for `/consolidate` is a **Spotify user access token**; the frontend obtains it
via Authorization Code + PKCE (the backend itself only does client-credentials, for its own reads).

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

See [AGENTS.md](AGENTS.md) for architecture details and conventions if you're making changes.

## Note

This is the oldest project in this author's collection of personal services — the `backend/` module
is simpler and structured a bit differently (plain Spring MVC, single module) than its siblings
([chameidor](../../chameidor/README.md), [portfolio-2](../../portfolio-2/README.md)), which use a
Ktor-based multi-module architecture.
