// Hand-mirrors the label-follower backend DTOs. Keep in sync with
// backend/src/main/kotlin/com/rafaelfo/labelfollower/models/Track.kt and the JSON
// shapes returned by backend/.../api/*Controller.kt — in the SAME commit.
//
// The wire shape is camelCase: the Kotlin data class has no @JsonProperty, so
// Jackson serializes the property names verbatim.

export interface Track {
  name: string;
  isrc: string;
  spotifyId: string;
}

// --- one alias per endpoint -------------------------------------------------

/** GET /track/{isrc} */
export type TrackLookupResponse = Track;

/** GET /introspect/fromTrack/{isrc} — the label's recent catalogue (Set<Track> -> array). */
export type LabelCatalogResponse = Track[];

/** POST /introspect/newTracks/{isrc} — new-only; the backend also persists these server-side. */
export type DiscoverNewTracksResponse = Track[];

/**
 * POST /consolidate — no request body or auth header needed; the backend is permanently
 * authenticated against Spotify (see AuthController / SpotifyUserAuth). Returns HTTP 200 with
 * an EMPTY body: it walks every stored label, creates Spotify playlists, and only logs its
 * progress. No per-label result is returned.
 */
export type ConsolidateResponse = undefined;

/** GET /auth/spotify/status */
export interface SpotifyStatusResponse {
  connected: boolean;
}

/** POST /auth/spotify/exchange */
export interface ExchangeCodeRequest {
  code: string;
  redirectUri: string;
}

/** DELETE /auth/spotify */
export type DisconnectSpotifyResponse = undefined;

// `Label` (backend models/Label.kt) is never serialized by any endpoint, so it is
// intentionally not mirrored here.
