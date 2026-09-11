# label-follower — bugs e plano de melhorias

> Inventário levantado em 2026-09-10 a partir da leitura completa do código e de uma verificação com a
> API real do Spotify ([tecnico.md §11](tecnico.md#11-estado-verificado-em-2026-09-10)). Regras de
> negócio citadas (RN-xx) estão em [negocio.md](negocio.md).
>
> **Prioridade:** P0 crítico (quebra o produto ou perde dados) · P1 alto · P2 médio (risco de negócio) · P3 qualidade.
> **Evidência:** 🧪 reproduzido em teste · 📖 identificado lendo o código.
> **Status:** ✅ corrigido (com o PR) · em branco = aberto.

## Resumo

| # | Prioridade | Evidência | Problema | Status |
|---|---|---|---|---|
| [B1](#b1) | P0 | 🧪 | Imagem Docker não consegue chamar o Spotify (`apiUri` de produção) | ✅ #19 |
| [B2](#b2) | P0 | 🧪 | Consolidar perde faixas novas quando algo falha | |
| [B3](#b3) | P0 | 📖 | Erros HTTP do Spotify são ignorados | ✅ #19 |
| [B19](#b19) | P0 | 🧪 | Client secret e tokens impressos nos logs | ✅ #19 |
| [B4](#b4) | P1 | 📖 | Data de lançamento só com ano/mês derruba o selo | |
| [B5](#b5) | P1 | 📖 | Álbum sem copyright/selo derruba o selo | |
| [B6](#b6) | P1 | 🧪 | Faixas duplicadas (mesmo ISRC, ids diferentes) | |
| [B7](#b7) | P1 | 🧪 | Todo erro vira 500 sem mensagem útil | |
| [B8](#b8) | P1 | 📖 | Consolidar síncrono, sem trava contra execução dupla | |
| [B9](#b9) | P1 | 📖 | Conectar outra conta mistura as duas | |
| [B10](#b10) | P1* | 📖 | API sem autenticação, OAuth sem `state`, MySQL exposto sem senha | |
| [B11](#b11)–[B18](#b18) | P2 | 📖 | Riscos de negócio (copyright, janela de datas, selo errado, playlists…) | |
| [P3](#p3) | P3 | 📖/🧪 | Qualidade de código e infraestrutura | |

\* B10 sobe para P0 se o app estiver exposto na internet.

---

## P0 — críticos

### <a id="b1"></a>B1 · Imagem Docker não consegue chamar o Spotify 🧪 ✅

- **Status:** corrigido no #19. O arquivo de produção só sobrescreve o MySQL e herda `spotify.*`;
  `SpotifyPropertiesTest` garante que os dois perfis resolvem a mesma base. Verificado com o perfil
  `production` contra a API real: `/track` e `/introspect/fromTrack` respondem `200`.
- **Onde (antes da correção):** `backend/src/main/resources/application-production.properties:4`, `RafaHttp.kt:44-47`.
- **O que acontece:** o perfil `production` (ativado pelo `Dockerfile`) define
  `spotify.apiUri=https://api.spotify.com/v1/`. O `RafaHttp.get` separa a URL em `scheme` e `host`
  com `split("://")` e passa `api.spotify.com/v1/` como host → `IllegalArgumentException: unexpected host`.
  Os POSTs montam `https://api.spotify.com/v1//v1/users/...`.
- **Impacto:** na imagem publicada, busca de faixa, catálogo, descoberta, conexão da conta (`/v1/me`)
  e consolidar falham. Só `/auth/spotify/status` funciona.
- **Correção:** o arquivo de produção passa a conter só o que difere (MySQL sem defaults); montar URLs
  com `baseUrl.toHttpUrl().newBuilder().addPathSegments(...)`, que aceita base com caminho e porta.
  Testes da montagem de URL com MockWebServer.

### <a id="b2"></a>B2 · Consolidar perde faixas novas quando algo falha 🧪

- **Onde:** `LabelIntrospector.kt:21-23`, `Consolidator.kt:15-19`.
- **O que acontece:** `discoverNewTracksFrom` grava as faixas como conhecidas **antes** de a playlist
  existir, e o `associateWith` do `Consolidator` descobre e grava **todos** os selos antes de criar a
  primeira playlist. Qualquer falha depois (conta não conectada, refresh token revogado, 429/5xx do
  Spotify, exceção no selo N) deixa as faixas marcadas como conhecidas — e elas nunca mais aparecem.
- **Reprodução:** banco com 5 faixas "esquecidas" + `POST /consolidate` sem conta conectada → `500`,
  e os vínculos passaram de 158 para 163. O segundo consolidar devolve `200` sem nada.
- **Correção:** separar "descobrir" (sem efeito colateral) de "marcar como conhecidas". No consolidar,
  por selo: descobrir → criar a playlist → só então gravar. Um selo que falha não afeta os outros e é
  tentado de novo na próxima execução. `POST /introspect/newTracks` mantém o comportamento atual
  (descobrir + gravar).

### <a id="b3"></a>B3 · Erros HTTP do Spotify são ignorados 📖 ✅

- **Status:** corrigido no #19. O `RafaHttp` devolve um `HttpResult` já lido e fechado e lança
  `ExternalServiceException(method, url, status, body)` fora de 2xx. Ele repete 429 respeitando
  `Retry-After` e repete 502/503/504 **só em GET**, porque um POST pode ter criado a playlist. Usa um
  `OkHttpClient` compartilhado e envia `Content-Type: application/json`. Na verificação, um `502`
  transitório do Spotify apareceu como `ExternalServiceException … HTTP 502`, e não mais como NPE.
- **Onde (antes da correção):** `RafaHttp.kt`, `ResponseHelpers.kt:7-8`, `SpotifyUserPlaylistGateway.kt:32-38`.
- **O que acontece:** o status HTTP nunca é verificado. O Gson faz parse do corpo de erro e, como
  ignora a nulidade do Kotlin, gera objetos com campos nulos que explodem depois, longe da origem
  (ex.: trocar um código OAuth inválido termina num erro de `NOT NULL` do banco). A resposta de
  "adicionar faixas à playlist" nunca é verificada nem fechada: faixas podem não entrar sem aviso.
  Não há tratamento de `429 Too Many Requests`/`Retry-After`, e cada requisição cria um `OkHttpClient` novo.
### <a id="b19"></a>B19 · Client secret e tokens impressos nos logs 🧪 ✅

- **Status:** corrigido no #19. O log agora é `GET <url> -> <status> (<ms> ms)` via SLF4J; um teste com
  appender do Logback garante que nenhum valor de header aparece. Verificado com o perfil `production`:
  nenhuma ocorrência de `Authorization`, `Bearer` ou `Basic` no log.
- **Onde (antes da correção):** `RafaHttp.kt:77` — `println("RafaHttp: $this")`.
- **O que acontece:** o `Request.toString()` do OkHttp 4.12 inclui os headers **sem mascarar**. Cada
  pedido de token imprime `Authorization: Basic base64(clientId:clientSecret)`, e cada chamada imprime o
  `Bearer` (do app ou do usuário). Na imagem Docker, isso vai para os logs do container.
- **Ação fora do código:** se logs de algum ambiente com a imagem antiga foram guardados ou
  compartilhados, gere um novo client secret no painel do Spotify.

## P1 — altos

### <a id="b4"></a>B4 · Data de lançamento só com ano/mês derruba o selo 📖

`SpotifyGateway.kt:52-58` interpreta `release_date` com `ISO_LOCAL_DATE`, mas o Spotify devolve datas
com precisão `year` (`1998`) ou `month` (`1998-05`) em lançamentos antigos. A busca por selo traz
também álbuns antigos, então basta um para a descoberta inteira do selo falhar com `500`.
**Correção:** interpretar conforme `release_date_precision` (completando mês/dia); se não der para
interpretar, tratar como antigo. Teste com os três formatos.

### <a id="b5"></a>B5 · Álbum sem copyright/selo derruba o selo 📖

`Label.matches` (`Label.kt:12-18`) usa `reduce`, que lança exceção com lista vazia; `SpotifyAlbum.toLabel`
usa `copyrights!!` e `label!!` (`SpotifyAlbum.kt:17,23`). **Correção:** `any {}`; álbum sem esses dados
é descartado (não casa); nome comparado sem diferenciar maiúsculas.

### <a id="b6"></a>B6 · Faixas duplicadas 🧪

A mesma gravação (ISRC) aparece com ids diferentes do Spotify (single, álbum, coletânea). Como `Track`
compara também o `spotifyId`, as duas cópias viram faixas "novas" distintas → aparecem duas vezes na
lista e na playlist, e o React reclama de `key` duplicada (`TrackList.tsx:15`). No teste, 9 ISRCs de
163 estavam duplicados. **Correção:** deduplicar por ISRC no `SpotifyGateway.getTracksFrom`, preferindo
o lançamento mais antigo.

### <a id="b7"></a>B7 · Todo erro vira 500 sem mensagem útil 🧪

`SpotifyTrackGateway.kt:27` usa `.first()` (ISRC inexistente → `NoSuchElementException` → `500`). Não há
`@ControllerAdvice`, e o Spring omite `message` fora do devtools: a UI mostra "Internal Server Error", e o
tratamento de 404 do front (`Explorer.tsx:82`) nunca dispara. **Correção:** `@RestControllerAdvice`
mapeando não encontrado → `404`, erro do Spotify → `502`, sem conta conectada → `409`, com corpo `{ message }`.

### <a id="b8"></a>B8 · Consolidar síncrono, sem trava contra execução dupla 📖

`POST /consolidate` roda o processo inteiro dentro da requisição. Atrás do nginx (`proxy_read_timeout`
padrão de 60 s), com vários selos o navegador recebe `504` enquanto o backend continua. Nada impede duas
execuções simultâneas (duplo clique, duas abas) → playlists duplicadas e possível violação de chave única
em `upsertTrack`. **Correção:** execução em segundo plano com trava: `POST` → `202` + id;
`GET /consolidate/{id}` → status e resumo (selos, faixas por selo, links das playlists). O resumo
substitui o `println`, e a tela passa a mostrar o resultado.

### <a id="b9"></a>B9 · Conectar outra conta mistura as duas 📖

`SpotifyUserAuth.connect` faz upsert por `spotify_user_id`: outra conta vira uma **segunda** linha, mas
`getSpotifyUserId` e o refresh leem `all().firstOrNull()` (a antiga). Resultado: id da conta antiga com
o token da nova → `403` ao criar playlist; depois de reiniciar, volta a usar a conta antiga.
**Correção:** `connect` substitui (apaga tudo e insere) numa transação; teste com Testcontainers.

### <a id="b10"></a>B10 · Segurança de acesso 📖

- A API não tem autenticação: quem alcança o host pode consolidar (criando playlists na conta dona),
  desconectar a conta ou conectar outra.
- O OAuth não usa `state` (`frontend/src/lib/spotifyAuth.ts:19-24`), o que permite CSRF de login.
- O `docker-compose.yml` da raiz publica o MySQL (`3306`, root **sem senha**) e a API (`8080`, sem
  passar pelo nginx) em todas as interfaces.
- O refresh token fica em texto puro no banco.

**Correção mínima:** `state` no OAuth (gerado e conferido no front); não publicar 3306/8080, ou publicar
só em `127.0.0.1`; senha no MySQL. **Se o app estiver na internet:** autenticação na API (basic auth no
nginx ou token no Spring) antes de qualquer outra coisa.

## P2 — médios (risco de negócio)

- <a id="b11"></a>**B11 · Copyright com ano no meio do texto.** A limpeza (`SpotifyAlbum.kt:29`) só
  remove o ano **no início**. Com `℗ 2023 Selo`, o ano fica; como o Consolidar só usa copyrights já
  gravados, um álbum `℗ 2024 Selo` deixa de casar → lançamento perdido sem aviso. Normalizar os dois
  lados (remover ©, ℗, (C), (P) e anos em qualquer posição).
- <a id="b12"></a>**B12 · Janela "recente" fixa em 2021-11-01.** (`SpotifyGateway.kt:44`) O "recente"
  cresce para sempre: cada Consolidar relê ~5 anos por selo. A extensão `Instant.isAfter(String)`
  (`:48-50`) inverte o sentido do `isAfter` padrão e confunde a leitura. Tornar a janela configurável
  (ex.: últimos N dias ou desde a última execução do selo) e usar o filtro `year:` na busca — menos
  chamadas e sem esbarrar no teto de 1000 resultados (`SpotifyLabelGateway.kt:34`).
- <a id="b13"></a>**B13 · Selo da faixa-semente pode estar errado.** Usa o 1º resultado da busca por ISRC
  (`SpotifyTrackGateway.kt:27`), que pode ser uma coletânea de outro selo. Preferir `album`/`single` e o
  lançamento mais antigo.
- <a id="b14"></a>**B14 · Álbuns com mais de 50 faixas truncados.** O `GET /v1/albums` traz só as 50
  primeiras faixas (`tracks.items`); coletâneas grandes ficam incompletas. Decidir também se coletâneas
  devem contar como "novidade".
- <a id="b15"></a>**B15 · Playlists.** Uma playlist por selo a cada execução, nome `<Instant>-<selo>`
  (com `Instant.now()`, fora do `Clock` injetado), pública por padrão no Spotify e faixas em lotes de 5
  (a API aceita 100). Proposta: manter uma por selo e por execução, com nome legível
  (`<selo> — novidades AAAA-MM-DD`), privada e lotes de 100. Mudar a estratégia (ex.: uma playlist
  por execução ou uma fixa por selo) é uma decisão de produto e uma feature separada.
- <a id="b16"></a>**B16 · Colunas `varchar(255)`** para nome de faixa e texto de copyright: um valor
  maior aborta a transação inteira do selo.
- <a id="b17"></a>**B17 · Cache de token sem trava e sem margem** (`SpotifyAuth`, `SpotifyUserAuth`): o
  token pode expirar no meio de um Consolidar longo; chamadas concorrentes pedem tokens em paralelo.
  Adicionar margem (~60 s) e `@Synchronized`.
- <a id="b18"></a>**B18 · Status "conectado" otimista.** `/auth/spotify/status` só confere se existe
  linha no banco; um refresh token revogado continua aparecendo como conectado.

## <a id="p3"></a>P3 — qualidade de código e infraestrutura

- **Detekt só roda regras de formatação.** `disableDefaultRuleSets = true` (`build.gradle.kts:143`)
  desliga as regras configuradas no `config.yml`. Evidência: `MagicNumber` está ativo lá, e o `1000` em
  `SpotifyLabelGateway.kt:34` não foi apontado (0 findings em 41 arquivos). Religar num PR próprio,
  corrigindo os findings ou criando um baseline.
- `println` em vez de SLF4J em `SpotifyAuth` e `Consolidator` (o `RafaHttp` já usa SLF4J desde o #19);
  Gson em DTOs Kotlin (Jackson + `jackson-module-kotlin` já estão no classpath).
- Dependências sem uso: `kotest-extensions-spring:4.4.3` (artefato do Kotest 4 junto com o 5.9),
  `kotlinx-coroutines`, `spring-boot-starter-validation`, `exposed-json`; `profiles/Development.kt` sem uso;
  testes usam `coEvery`/`coVerify` em funções que não são `suspend`.
- Sem pool de conexões: `Database.connect(url)` abre uma conexão JDBC por transação → HikariCP.
- `OurInfoGateway.getTracksFrom` é uma leitura que **cria** selo (`upsertLabel`).
- Chamadas redundantes: a busca por ISRC é feita 2× no backend em `/introspect/*` (+1 pelo `/track` do
  front); lotes de `tracks?ids` com 20 (a API aceita 50).
- Healthcheck do Docker só olha o nginx (`Dockerfile:69-70`): se a API cair, o container segue "healthy".
- `.claude/launch.json`: o backend roda `./gradlew -p backend` a partir da raiz (onde não existe `gradlew`),
  e o front aponta o proxy para a `8097` enquanto o backend sobe na `8080`.
- `vite.config.ts` lê `process.env.VITE_API_TARGET`, mas o Vite não carrega `.env` no config → o valor em
  `.env.development` é ignorado (usar `loadEnv`).
- Front: `useDiscoverNewTracks` invalida o catálogo sem necessidade (`queries.ts:34-36`), refazendo ~16
  chamadas ao Spotify; sem testes nem ESLint.
- Cobertura de testes: nada para `Consolidator`, `Label.matches`, `SpotifyAlbum.toLabel`, filtros de data,
  `OurInfoGatewayImpl`, `SpotifyUserAuth`, `SpotifyUserPlaylistGateway` e controllers.

---

## Plano de execução

PRs pequenos, uma branch por PR (regra do repositório), sempre com um teste que falhava antes da
correção. Mudança de contrato da API atualiza `frontend/src/api/types.ts` no mesmo commit.

| Ordem | PR | Itens | Observação |
|---|---|---|---|
| 1 | Documentação | este diretório `docs/`, docs obsoletas, remoção de `backend/mysql/init.sql` | |
| 2 | Camada HTTP | B1, B3, B19 | ✅ #19 — MockWebServer; base para testar todos os gateways |
| 3 | Atomicidade do Consolidar | B2 | testes do `Consolidator` com gateway de playlist falhando |
| 4 | Robustez do catálogo | B4, B5, B6 | testes com os formatos reais do Spotify |
| 5 | Erros da API | B7 | `@RestControllerAdvice`; front passa a mostrar a mensagem |
| 6 | Conta Spotify | B9, B17, B18 | Testcontainers para `SpotifyUserAuth` |
| 7 | Consolidar em segundo plano | B8 (+ parte de B15) | novo contrato de API → atualizar o front junto |
| 8 | Segurança | B10 | antes do 2 se o app estiver exposto na internet |
| 9 | Regras de negócio | B11–B16 | cada uma com decisão de produto |
| 10 | Qualidade | P3 | detekt em PR isolado |

### Verificação de cada PR

- `npm run check` na raiz (`detekt test` + typecheck + build) e CI verde.
- Teste novo falhando antes e passando depois.
- Smoke dos fluxos afetados em dev; para B1, B8 e B10, também com o perfil `production`/imagem Docker.
