import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./client.ts";
import { isValidIsrc, normalizeIsrc } from "../lib/isrc.ts";
import { getFreshAccessToken } from "../lib/spotifyAuth.ts";

export const keys = {
  track: (isrc: string) => ["track", isrc] as const,
  labelCatalog: (isrc: string) => ["introspect", "fromTrack", isrc] as const,
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

/**
 * POST /consolidate. Pulls a fresh Spotify user token (refreshing via PKCE if
 * needed); throws if the user has not connected Spotify. Returns no result body.
 */
export function useConsolidate() {
  return useMutation({
    mutationFn: async () => {
      const token = await getFreshAccessToken();
      if (!token) throw new Error("Conecte sua conta do Spotify primeiro.");
      return api.consolidate(token);
    },
  });
}
