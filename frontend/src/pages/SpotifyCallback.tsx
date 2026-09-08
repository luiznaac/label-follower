import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { completeSpotifyLogin } from "../lib/spotifyAuth.ts";

export function SpotifyCallback() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return; // StrictMode double-invoke guard — a code is single-use
    ran.current = true;

    const params = new URLSearchParams(window.location.search);
    const denied = params.get("error");
    const code = params.get("code");

    if (denied) {
      setError(`Spotify recusou a autorização (${denied}).`);
      return;
    }
    if (!code) {
      setError("Retorno do Spotify sem código de autorização.");
      return;
    }

    completeSpotifyLogin(code)
      .then(() => navigate("/consolidate", { replace: true }))
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Falha ao conectar."));
  }, [navigate]);

  return (
    <div className="grid min-h-screen place-items-center px-4">
      {error ? (
        <div className="space-y-3 text-center">
          <p className="text-sm text-red-400">{error}</p>
          <a href={`${import.meta.env.BASE_URL}consolidate`} className="text-sm text-brand-400 hover:underline">
            Voltar e tentar novamente
          </a>
        </div>
      ) : (
        <p className="text-sm text-neutral-400">Conectando ao Spotify…</p>
      )}
    </div>
  );
}
