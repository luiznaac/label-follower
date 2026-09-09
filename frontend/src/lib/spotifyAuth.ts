// Kicks off the browser redirect to Spotify's consent screen. The authorization code that
// comes back is exchanged server-side (see api/queries.ts useExchangeSpotifyCode /
// AuthController.kt on the backend) — the backend holds the resulting refresh token and stays
// authenticated permanently, so this file no longer stores or refreshes any token itself.
// Same registered redirect URI as before this change (frontend/README.md's Spotify app setup
// note still applies).

const CLIENT_ID = import.meta.env.VITE_SPOTIFY_CLIENT_ID ?? "5a5af3ea4d104213872ebff79136fcda";

export const SPOTIFY_REDIRECT_URI =
  import.meta.env.VITE_SPOTIFY_REDIRECT_URI ??
  `${window.location.origin}${import.meta.env.BASE_URL}callback`;

const SCOPES = "playlist-modify-public playlist-modify-private";
const AUTHORIZE_URL = "https://accounts.spotify.com/authorize";

/** Redirect to Spotify's consent screen. */
export function beginSpotifyLogin(): void {
  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    response_type: "code",
    redirect_uri: SPOTIFY_REDIRECT_URI,
    scope: SCOPES,
  });
  window.location.assign(`${AUTHORIZE_URL}?${params.toString()}`);
}
