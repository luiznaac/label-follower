import type { Track } from "../api/types.ts";

interface Props {
  tracks: Track[];
  empty?: string;
}

export function TrackList({ tracks, empty = "Nenhuma faixa." }: Props) {
  if (tracks.length === 0) {
    return <p className="text-sm text-neutral-500">{empty}</p>;
  }
  return (
    <ul className="divide-y divide-surface-800 overflow-hidden rounded-lg border border-surface-800">
      {tracks.map((t) => (
        <TrackRow key={t.isrc || t.spotifyId} track={t} />
      ))}
    </ul>
  );
}

function TrackRow({ track }: { track: Track }) {
  return (
    <li className="flex items-center justify-between gap-3 bg-surface-850 px-4 py-3">
      <div className="min-w-0">
        <p className="truncate text-sm text-neutral-100">{track.name}</p>
        <p className="font-mono text-xs text-neutral-500">{track.isrc}</p>
      </div>
      {track.spotifyId && (
        <a
          href={`https://open.spotify.com/track/${track.spotifyId}`}
          target="_blank"
          rel="noreferrer"
          className="shrink-0 text-xs font-medium text-brand-400 hover:underline"
        >
          Abrir no Spotify
        </a>
      )}
    </li>
  );
}
