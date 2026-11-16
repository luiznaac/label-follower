# Spotify app runbook

The third-party steps the Quickstart cannot perform: creating the Spotify app, registering the dev redirect URI and rotating the client secret. The commands themselves live in the README.

## Credentials

- The app's **client ID** is public and lives in `backend/src/main/resources/application.properties` (`spotify.clientId`).
- The matching **client secret** is supplied as the `SPOTIFY_CLIENT_SECRET` environment variable. Locally it comes from the gitignored `.env` (copy `.env.example`); the full-stack compose file requires it.
- There is no fake fixture: Spotify rejects an invalid client secret, so dev needs a real value.

## Redirect URI

Spotify matches the redirect URI exactly and rejects `http://localhost`.

1. Open the Spotify Developer Dashboard → the label-follower app → Settings → Redirect URIs.
2. Add `http://127.0.0.1:8123/callback`.
3. Save.

The Vite dev server binds `127.0.0.1` on port `8123` with `strictPort: true`, so the served origin cannot drift away from the registered URI.

## Login flow

The Consolidate screen sends the browser to Spotify's consent screen. The `code` comes back to `/callback`, is exchanged server-side, and the backend keeps the login used for playlist creation. The browser never stores a token.

## Rotating the secret

1. Reset the client secret in the Spotify dashboard.
2. Update the `SPOTIFY_CLIENT_SECRET` value in `.env` locally, or in the deployment's secret store.
3. Restart the backend.

## Reference

- [Spotify app documentation](https://developer.spotify.com/documentation/web-api/concepts/apps)
