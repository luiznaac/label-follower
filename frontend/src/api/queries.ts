import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./client.ts";
import { isValidIsrc, normalizeIsrc } from "../lib/isrc.ts";

export const keys = {
  track: (isrc: string) => ["track", isrc] as const,
  labelCatalog: (isrc: string) => ["introspect", "fromTrack", isrc] as const,
  spotifyStatus: ["auth", "spotify", "status"] as const,
};

export function useTrack(rawIsrc: string) {
  const isrc = normalizeIsrc(rawIsrc);
  return useQuery({
    queryKey: keys.track(isrc),
    queryFn: () => api.getTrack(isrc),
    enabled: isValidIsrc(isrc),
  });
}

export function useLabelCatalog(rawIsrc: string) {
  const isrc = normalizeIsrc(rawIsrc);
  return useQuery({
    queryKey: keys.labelCatalog(isrc),
    queryFn: () => api.getLabelCatalog(isrc),
    enabled: isValidIsrc(isrc),
  });
}

/** POST /introspect/newTracks/{isrc} — a mutation, since it writes on the server. */
export function useDiscoverNewTracks() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (rawIsrc: string) => api.discoverNewTracks(normalizeIsrc(rawIsrc)),
    onSuccess: (_data, rawIsrc) => {
      qc.invalidateQueries({ queryKey: keys.labelCatalog(normalizeIsrc(rawIsrc)) });
    },
  });
}

/** Whether the backend currently holds a working Spotify login. */
export function useSpotifyStatus() {
  return useQuery({
    queryKey: keys.spotifyStatus,
    queryFn: () => api.getSpotifyStatus(),
  });
}

/** Trades an OAuth `code` for a permanent backend Spotify login (see SpotifyCallback.tsx). */
export function useExchangeSpotifyCode() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (params: { code: string; redirectUri: string }) => api.exchangeSpotifyCode(params),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.spotifyStatus }),
  });
}

/** Forgets the backend's stored Spotify login. */
export function useDisconnectSpotify() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => api.disconnectSpotify(),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.spotifyStatus }),
  });
}

/** POST /consolidate. Requires the backend to already have a Spotify login (useSpotifyStatus). */
export function useConsolidate() {
  return useMutation({
    mutationFn: () => api.consolidate(),
  });
}
