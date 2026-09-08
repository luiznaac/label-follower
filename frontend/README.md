# label-follower-fe

Frontend do [`label-follower`](../backend). SPA em React + Vite + TypeScript +
Tailwind v4, consumindo a API HTTP do backend. Mesma stack e convenções do
[`shougong/frontend`](../../shougong/frontend).

Estética escura, sóbria, com acento verde estilo Spotify.

## Pré-requisitos

- **Node.js 20+**.
- O backend rodando: `cd backend && SPOTIFY_CLIENT_SECRET=… ./gradlew bootRun`
  (→ `http://localhost:8080`).
- Para a tela **Consolidar**: registre `http://127.0.0.1:5274/callback` como
  _Redirect URI_ no app do Spotify (client id `5a5af3ea4d104213872ebff79136fcda`).
  O Spotify recusa `http://localhost` — precisa ser o IP literal.

## Rodar em dev

```bash
npm install
npm run dev
```

Abre em `http://127.0.0.1:5274`. As chamadas para `/api/*` são _proxied_ para
`http://localhost:8080` e o prefixo `/api` é removido (config em `vite.config.ts`,
ajustável por `VITE_API_TARGET`). Assim não precisa mexer em CORS no backend.

> No PowerShell, se `npm` for bloqueado pela _execution policy_
> (`npm.ps1 cannot be loaded`), use `npm.cmd install` / `npm.cmd run dev`, ou
> libere com `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

## Build

```bash
npm run build      # gera dist/ com base path /label-follower/ (para o reverse proxy)
npm run preview
```

Para buildar na raiz (`/`) em vez de `/label-follower/`: `VITE_BASE=/ npm run build`
(é o que o `Dockerfile` faz).

## Telas

| Rota           | O quê                                                                     |
| -------------- | ------------------------------------------------------------------------ |
| `/`            | Painel: status da conexão com o Spotify e atalhos para as outras telas   |
| `/introspect`  | Busca uma faixa por ISRC, mostra o catálogo recente do selo e descobre faixas novas |
| `/consolidate` | Conecta o Spotify e dispara `POST /consolidate` para todas as gravadoras |
| `/callback`    | Alvo do redirect OAuth do Spotify — troca o `code` e volta para `/consolidate` |

## Notas de arquitetura

- `src/api/` — cliente HTTP tipado (`client.ts`) + hooks TanStack Query
  (`queries.ts`). Os tipos em `types.ts` **espelham** `models/Track.kt` e as
  respostas dos controllers do backend; qualquer mudança nos dois lados vai no
  mesmo commit (não há codegen).
- `src/lib/spotifyAuth.ts` — o token de usuário do Spotify para `POST /consolidate`
  é obtido **inteiramente no navegador** via Authorization Code + PKCE
  (`accounts.spotify.com`, com CORS liberado para clientes PKCE públicos). O
  backend continua usando só client-credentials para as leituras dele. O token
  (access + refresh) fica em `localStorage`.
- `src/lib/isrc.ts` — normaliza (maiúsculas, sem hífens) e valida ISRC; as queries
  só disparam com um ISRC válido.
- `POST /consolidate` responde `200` sem corpo — a tela só mostra
  iniciado / concluído / erro. A criação de playlist usa um usuário do Spotify
  fixo no backend (`SpotifyUserPlaylistGateway`), então na prática só essa conta
  consegue consolidar.
