import { useState } from "react";
import { beginSpotifyLogin } from "../lib/spotifyAuth.ts";

export function SpotifyConnectButton() {
  const [pending, setPending] = useState(false);

  function connect() {
    setPending(true);
    beginSpotifyLogin(); // redirects away
  }

  return (
    <button
      type="button"
      onClick={connect}
      disabled={pending}
      className="rounded-full bg-brand-500 px-5 py-2.5 text-sm font-semibold text-surface-950 transition-colors hover:bg-brand-400 disabled:opacity-50"
    >
      {pending ? "Redirecionando…" : "Conectar Spotify"}
    </button>
  );
}
