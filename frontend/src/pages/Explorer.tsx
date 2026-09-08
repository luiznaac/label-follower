import { useSearchParams } from "react-router-dom";
import { IsrcInput } from "../components/IsrcInput.tsx";
import { TrackList } from "../components/TrackList.tsx";
import { ApiError } from "../api/client.ts";
import { useDiscoverNewTracks, useLabelCatalog, useTrack } from "../api/queries.ts";

export function Explorer() {
  const [params, setParams] = useSearchParams();
  const isrc = params.get("isrc") ?? "";

  const track = useTrack(isrc);
  const catalog = useLabelCatalog(isrc);
  const discover = useDiscoverNewTracks();

  function search(next: string) {
    discover.reset();
    setParams(next ? { isrc: next } : {});
  }

  return (
    <div className="space-y-8">
      <section className="space-y-3">
        <h1 className="text-xl font-semibold text-neutral-100">Explorar selo</h1>
        <IsrcInput key={isrc} initial={isrc} onSubmit={search} />
      </section>

      {isrc && (
        <>
          <section className="space-y-2">
            <h2 className="text-sm font-medium uppercase tracking-wide text-neutral-500">Faixa</h2>
            {track.isPending && <p className="text-sm text-neutral-500">Buscando…</p>}
            {track.isError && <ErrorLine error={track.error} />}
            {track.data && (
              <div className="rounded-lg border border-surface-800 bg-surface-850 p-4">
                <p className="text-neutral-100">{track.data.name}</p>
                <p className="font-mono text-xs text-neutral-500">{track.data.isrc}</p>
              </div>
            )}
          </section>

          <section className="space-y-2">
            <h2 className="text-sm font-medium uppercase tracking-wide text-neutral-500">
              Catálogo recente do selo
            </h2>
            {catalog.isPending && <p className="text-sm text-neutral-500">Buscando…</p>}
            {catalog.isError && <ErrorLine error={catalog.error} />}
            {catalog.data && (
              <TrackList tracks={catalog.data} empty="O selo não tem lançamentos recentes." />
            )}
          </section>

          <section className="space-y-3">
            <div className="flex items-center gap-3">
              <button
                onClick={() => discover.mutate(isrc)}
                disabled={discover.isPending}
                className="rounded-md bg-brand-500 px-4 py-2 text-sm font-semibold text-surface-950 transition-colors hover:bg-brand-400 disabled:opacity-40"
              >
                {discover.isPending ? "Procurando…" : "Descobrir faixas novas"}
              </button>
              <span className="text-xs text-neutral-500">
                Compara com o que já está registrado e salva as novidades no servidor.
              </span>
            </div>
            {discover.isError && <ErrorLine error={discover.error} />}
            {discover.data && (
              <TrackList
                tracks={discover.data}
                empty="Nada novo — o selo não lançou faixas desconhecidas."
              />
            )}
          </section>
        </>
      )}
    </div>
  );
}

function ErrorLine({ error }: { error: unknown }) {
  let message = "Algo deu errado.";
  if (error instanceof ApiError) {
    message = error.status === 404 ? "ISRC não encontrado no Spotify." : error.message;
  } else if (error instanceof Error) {
    message = error.message;
  }
  return <p className="text-sm text-red-400">{message}</p>;
}
