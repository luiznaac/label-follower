import { SpotifyConnectButton } from "../components/SpotifyConnectButton.tsx";
import { useConsolidate, useDisconnectSpotify, useSpotifyStatus } from "../api/queries.ts";

export function Consolidate() {
  const status = useSpotifyStatus();
  const disconnect = useDisconnectSpotify();
  const consolidate = useConsolidate();

  return (
    <div className="space-y-6">
      <section className="space-y-2">
        <h1 className="text-xl font-semibold text-neutral-100">Consolidar</h1>
        <p className="max-w-prose text-sm text-neutral-400">
          Percorre todas as gravadoras conhecidas, procura faixas novas em cada uma e cria uma
          playlist no Spotify com o resultado.
        </p>
      </section>

      {status.isPending && <p className="text-sm text-neutral-500">Verificando conexão…</p>}

      {status.isSuccess && !status.data.connected && <SpotifyConnectButton />}

      {status.isSuccess && status.data.connected && (
        <div className="space-y-5">
          <div className="flex items-center gap-3 text-sm">
            <span className="text-brand-400">● Spotify conectado</span>
            <button
              onClick={() => disconnect.mutate()}
              disabled={disconnect.isPending}
              className="text-neutral-500 hover:text-neutral-300 disabled:opacity-50"
            >
              Desconectar
            </button>
          </div>

          <button
            onClick={() => consolidate.mutate()}
            disabled={consolidate.isPending}
            className="rounded-full bg-brand-500 px-5 py-2.5 text-sm font-semibold text-surface-950 transition-colors hover:bg-brand-400 disabled:opacity-50"
          >
            {consolidate.isPending ? "Consolidando…" : "Consolidar todas as gravadoras"}
          </button>

          {consolidate.isPending && (
            <p className="max-w-prose rounded-md border border-surface-800 bg-surface-850 p-3 text-xs text-neutral-400">
              Isso percorre cada gravadora e cria playlists na sua conta do Spotify. Pode levar
              alguns minutos e não retorna um detalhamento — acompanhe o console do backend.
            </p>
          )}
          {consolidate.isSuccess && (
            <p className="text-sm text-brand-400">
              Pronto — confira as playlists novas no seu Spotify.
            </p>
          )}
          {consolidate.isError && (
            <p className="text-sm text-red-400">
              {consolidate.error instanceof Error
                ? consolidate.error.message
                : "Falha ao consolidar."}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
