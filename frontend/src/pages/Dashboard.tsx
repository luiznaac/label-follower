import { Link } from "react-router-dom";
import { StatTile } from "../components/StatTile.tsx";
import { useSpotifyStatus } from "../api/queries.ts";

export function Dashboard() {
  const status = useSpotifyStatus();
  const connected = status.data?.connected ?? false;

  return (
    <div className="space-y-8">
      <section className="space-y-2">
        <h1 className="text-2xl font-semibold text-neutral-100">label-follower</h1>
        <p className="max-w-prose text-sm text-neutral-400">
          Acompanhe as gravadoras que você segue no Spotify. Parta de uma faixa, descubra qual
          selo a lançou, veja o catálogo recente e encontre os lançamentos que ainda não estavam
          registrados — depois monte uma playlist com tudo que é novo.
        </p>
      </section>

      <section className="grid gap-3 sm:grid-cols-2">
        <StatTile
          label="Conta do Spotify"
          value={status.isPending ? "verificando…" : connected ? "conectada" : "desconectada"}
          tone={connected ? "brand" : "muted"}
        />
        <StatTile label="Ambiente" value={import.meta.env.DEV ? "dev" : "prod"} tone="muted" />
      </section>

      <section className="grid gap-3 sm:grid-cols-2">
        <NavCard
          to="/introspect"
          title="Explorar"
          body="Busque uma faixa por ISRC, veja o catálogo recente do selo e descubra faixas novas."
        />
        <NavCard
          to="/consolidate"
          title="Consolidar"
          body="Percorre todas as gravadoras conhecidas e cria playlists no Spotify com o que há de novo."
        />
      </section>
    </div>
  );
}

function NavCard({ to, title, body }: { to: string; title: string; body: string }) {
  return (
    <Link
      to={to}
      className="block rounded-lg border border-surface-800 bg-surface-850 p-4 transition-colors hover:border-brand-500"
    >
      <p className="font-semibold text-neutral-100">{title}</p>
      <p className="mt-1 text-sm text-neutral-400">{body}</p>
    </Link>
  );
}
