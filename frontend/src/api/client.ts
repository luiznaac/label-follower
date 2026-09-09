import type {
  ConsolidateResponse,
  DisconnectSpotifyResponse,
  DiscoverNewTracksResponse,
  ExchangeCodeRequest,
  LabelCatalogResponse,
  SpotifyStatusResponse,
  TrackLookupResponse,
} from "./types.ts";

const BASE = (import.meta.env.VITE_API_BASE ?? "/api").replace(/\/$/, "");

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });

  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      // Spring's default error body: { timestamp, status, error, message, path }
      detail = body.message ?? body.error ?? detail;
    } catch {
      /* non-JSON / empty body */
    }
    throw new ApiError(res.status, detail);
  }

  // /consolidate returns 200 with no body; several endpoints could 204. Guard .json().
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  /** GET /track/{isrc} */
  getTrack(isrc: string): Promise<TrackLookupResponse> {
    return request(`/track/${encodeURIComponent(isrc)}`);
  },

  /** GET /introspect/fromTrack/{isrc} — the label's recent catalogue. */
  getLabelCatalog(isrc: string): Promise<LabelCatalogResponse> {
    return request(`/introspect/fromTrack/${encodeURIComponent(isrc)}`);
  },

  /** POST /introspect/newTracks/{isrc} — also persists the new tracks server-side. */
  discoverNewTracks(isrc: string): Promise<DiscoverNewTracksResponse> {
    return request(`/introspect/newTracks/${encodeURIComponent(isrc)}`, { method: "POST" });
  },

  /**
   * POST /consolidate. The backend is permanently authenticated against Spotify server-side —
   * no token to pass here anymore.
   */
  consolidate(): Promise<ConsolidateResponse> {
    return request(`/consolidate`, { method: "POST" });
  },

  /** GET /auth/spotify/status */
  getSpotifyStatus(): Promise<SpotifyStatusResponse> {
    return request(`/auth/spotify/status`);
  },

  /** POST /auth/spotify/exchange — trades the OAuth `code` for a permanent backend login. */
  exchangeSpotifyCode(body: ExchangeCodeRequest): Promise<void> {
    return request(`/auth/spotify/exchange`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  /** DELETE /auth/spotify — forgets the backend's stored Spotify login. */
  disconnectSpotify(): Promise<DisconnectSpotifyResponse> {
    return request(`/auth/spotify`, { method: "DELETE" });
  },
};
