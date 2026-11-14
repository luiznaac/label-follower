# AGENTS.md — label-follower (backend)

Development guidelines for anyone (human, agent, or tool) working in `backend/` (the Kotlin/Spring service). The repo is a two-project monorepo — see the root [AGENTS.md](../AGENTS.md) for the layout and the one cross-cutting rule, and [frontend/README.md](../frontend/README.md) for the SPA. Run backend commands from this directory (`cd backend && ./gradlew <task>`).

## Code map

- [`docs/code-map.md`](docs/code-map.md)

## What this service does

label-follower discovers new tracks from record labels the user follows on Spotify and builds a playlist out of them. Given a track (identified by ISRC), it can find which label released it, "introspect" that label to see what other tracks it has released, and diff those against tracks already known/stored to find genuinely new releases — then create a Spotify playlist with them.

## Architecture

**This is the oldest project in the `luiznaac` personal family and predates the `application/gateway/http-api/usecase/persistence` multi-module split used by [chameidor](../../chameidor/AGENTS.md) and [portfolio-2](../../portfolio-2/AGENTS.md). The repo was given a `backend/` + `frontend/` split to add a web UI (mirroring [shougong](../../shougong/AGENTS.md)), but `backend/` itself is still a single Gradle module using plain Spring MVC, not Ktor. Do not restructure it into the multi-module Ktor shape unless explicitly asked — that flat single-module layout is an intentional (if dated) characteristic, not a bug to fix.**

Single Gradle module, `backend/src/main/kotlin/com/rafaelfo/labelfollower/`:

- **`api/`** — `@RestController`s (Spring MVC, not Ktor). Thin: each endpoint delegates directly to a usecase class. `ConsolidatorController`, `IntrospectController`, `TrackController`, `AuthController`, `HealthController`.
- **`usecases/`** — business logic as `@Service`/plain classes (`Consolidator`, `LabelIntrospector`, `TrackFinder`), plus **port interfaces** implemented by the integrations layer: `OurInfoGateway` (persisted label/track info), `ExternalInfoGateway` (Spotify lookups for a single track), `UserInfoGateway` (playlist creation on the user's Spotify account), `HealthChecker` (one health component). `usecases/health/HealthHandler` aggregates the checkers, and `usecases/health/HealthTelemetry` owns the `health.check.*` instruments and transition records.
- **`integrations/`** — implementations of the usecase ports:
  - `integrations/database/` implements `OurInfoGateway` against MySQL via Exposed (`OurInfoGatewayImpl`, plus its `*Table`/`*Entity` classes — see `config/DatabaseConfig.kt` for the connection and the "Database migrations" section for how the schema itself is managed) and `HealthChecker` for the `mysql-connection` component (`MySqlConnectionHealthCheck`).
  - `integrations/spotify/` implements `ExternalInfoGateway`/`UserInfoGateway` against the real Spotify API (`SpotifyAlbumGateway`, `SpotifyTrackGateway`, `SpotifyLabelGateway`, `SpotifyUserPlaylistGateway`, `SpotifyAuth`, plus `models/`/`responses/` for the Spotify JSON shapes).
  - `integrations/httputils/` — small OkHttp wrapper (`RafaHttp`) and response helpers shared by the Spotify gateways.
- **`models/`** — plain domain data classes, `Label` and `Track`.
- **`application/`** — `Boot.kt`, the Spring Boot entry point.
- **`config/`, `profiles/`** — Spring `@Configuration`/`@Profile` setup (`Development.kt`).

Dependency direction is the same "interface in the consuming layer, implementation in the integration layer" idea as the other repos in this family — just without a separate Gradle module per layer. `OurInfoGateway`/`ExternalInfoGateway`/`UserInfoGateway` live in `usecases/`; `OurInfoGatewayImpl` and the Spotify gateways live in `integrations/`.

## Design principles

- **Controllers stay thin.** They extract request data (path variables, the `Authorization` header) and call straight into a usecase method — see `ConsolidatorController`, `IntrospectController`, `TrackController`. Put logic in `usecases/`, not in the controller.
- **Gateways are named for what they abstract, not for the vendor**, from the usecase's point of view: `OurInfoGateway` (our own stored data), `ExternalInfoGateway`/`UserInfoGateway` (third-party data). The Spotify-specific naming only appears once you're inside `integrations/spotify/`. Keep new integrations following this "generic port, vendor-specific implementation" split — don't have a usecase depend on a `Spotify*` type directly.
- **Bearer tokens are handled at the controller boundary.** `ConsolidatorController` strips the `Bearer ` prefix before passing the token down — follow this pattern for any new endpoint that needs the user's Spotify token, rather than parsing it deeper in the call stack.

## How to implement a new feature (walkthrough)

Example: adding a new way to discover tracks.

1. Add any new domain type to `models/` if needed (keep it a plain data class, like `Label`/`Track`).
2. If the feature needs new external data, add a method to an existing port interface in `usecases/` (or a new one, if it's a genuinely different concern), and implement it in the matching `integrations/` package.
3. Add the orchestration logic as a method on an existing usecase class (`Consolidator`, `LabelIntrospector`, `TrackFinder`) or a new `@Service` class in `usecases/` if it doesn't fit an existing one.
4. Expose it via a thin `@RestController` method in `api/`, following the existing controllers' style (constructor-injected usecase, minimal logic in the method body).
5. Add unit tests for the usecase logic (see "Testing") — don't skip this even though the project is small; it's the primary safety net here since there's no separate integration-test module.

## Code style

Detekt 1.23.6 + `detekt-formatting`, split `config/detekt/config.yml` + `format.yml` — but this is an **older iteration** of the shared template: `ForbiddenComment` still uses `values: ['FIXME:', 'STOPSHIP:', 'TODO:']` rather than the newer `comments: [{value: ...}]` structure used in `environments/kotlin`. **Do not "fix" this to match the other repos unless asked** — it's a known, harmless divergence, not a defect.

Run `./gradlew detekt` before finishing a change.

## Coding standards

- [`CODING_STANDARDS.md`](CODING_STANDARDS.md) — "the language-idiom rules the tooling does not enforce"

## Observability

- [`docs/observability.md`](docs/observability.md) — the normative logging & metrics contract (OpenTelemetry logs + metrics over OTLP, no traces); read it before adding or changing anything that emits telemetry
- The wiring lives in [`config/telemetry/`](src/main/kotlin/com/rafaelfo/labelfollower/config/telemetry): SDK build and `@PreDestroy` flush, resource, metric views, redaction, structured events, the servlet correlation filter and its boundary resolver.
- `usecases/telemetry/` holds the token-free contract constants (`MetricCatalog`, `TelemetryAttributes`) shared by wiring and tests — the OTel **API** is allowed there, the SDK and instrumentation are not.
- `usecases/telemetry/TelemetryLogging.kt` is the structured `event()` call — SLF4J is allowed there, the SDK is not.
- `integrations/httputils/OkHttpConfig.kt` is the one shared outbound client: OkHttp instrumentation plus `X-Request-ID` propagation.

## Testing

Kotest (`kotest-runner-junit5`, `kotest-extensions-spring`), MockK. Test source roots:

- `src/test/kotlin/com/rafaelfo/labelfollower/...` — every spec, **mirroring the main package layout**: a spec for `foo/Bar.kt` lives at `src/test/kotlin/foo/BarTest.kt`, in the same package.
- `src/test/resources/` — fixtures read from the test classpath (e.g. `mysql-test-conf/my.cnf` for the Testcontainers MySQL).

Search for an existing spec and an existing fixture **before creating a new one** — a near-duplicate spec is the usual miss.

Two lanes:

```bash
./gradlew unitTest --no-daemon   # Docker-free lane — the pre-commit hook runs this
./gradlew test --no-daemon       # every spec, Docker-backed ones included (the CI lane)
```

`unitTest` is a `Test` task over the same source set with the `integration` tag excluded. That tag is the convention for any spec that needs a Docker daemon: `MigrationSchemaTest` carries it today, as `@Tag("integration")` plus Kotest's `@Tags("integration")` — Kotest 5.9 filters on its own annotation and does not read the JUnit one. `test` still runs everything and depends on `dockerPreflight`, which probes `docker info` (30s limit) and fails in seconds — CLI missing, non-zero exit (a permission-denied socket included) or no return within 30s — with a message naming the Docker-free lane, instead of leaving an opaque `DockerClientProviderStrategy` error in the HTML report.

`test` also pins Testcontainers' `api.version` system property to the daemon's advertised Docker API version (`docker version --format '{{.Server.APIVersion}}'`). Testcontainers 1.21.3's shaded docker-java otherwise speaks API `1.32`, which Docker 29+ rejects outright — that probe keeps the suite portable instead of hardcoding a version that goes stale.

Existing tests to use as a style reference: `SpotifyAuthTest.kt`, `LabelIntrospectorTest.kt`, `TrackFinderTest.kt`; the telemetry contract suites live under `config/telemetry/`, `integrations/httputils/` and `usecases/health/`, with `api/HealthControllerTest.kt` covering the HTTP surface.

## Database migrations

The schema is versioned SQL under `src/main/resources/db/migration/V*.sql` — there is no more `mysql/init.sql`. Two tools, each doing one half of the job:

- **Exposed's migration module** (`config/migration/MigrationScripts.kt`) *generates* the SQL by diffing `integrations/database/AllTables.kt` (every `Table` object) against a live database. It never applies anything.
- **Flyway** (`config/migration/Migrator.kt`) *applies* those `V*.sql` files. It runs as a standalone `main()`, invoked from `deploy/entrypoint.sh` before the app starts — not from the Spring context — so a failed migration aborts the container instead of serving traffic against a stale schema. `baselineOnMigrate` means a database that already has the tables (e.g. a local volume from before migrations existed) gets stamped at V1 rather than re-running it.

Changing a table:

1. Edit the `Table` object in `integrations/database/` (and add it to `AllTables.kt` if it's new).
2. Point `MYSQL_HOST`/`MYSQL_USER`/`MYSQL_PASSWORD` at a database already migrated to head, then `./gradlew generateMigrationScript -Pname=V2__add_something` (or `npm run db:generate -- -Pname=V2__add_something` from the repo root). Review the generated `.sql` before committing — the diff is mechanical and won't know a rename is a rename rather than a drop-and-add.
3. `./gradlew migrate` (or `npm run db:migrate`) to apply it locally.

`config/migration/MigrationSchemaTest.kt` is the guard: it migrates a throwaway Testcontainers MySQL to head and asserts `MigrationUtils.statementsRequiredForDatabaseMigration(*allTables)` is empty. If a `Table` changes without a matching migration (or vice versa), this test fails. It's the only test in the repo that needs a Docker daemon, so it carries the `integration` tag (see "Testing") and only the `test` lane runs it — `unitTest` needs no Docker.

## Configuration

`src/main/resources/application.properties` (plus a `-production` variant):

| Key | Notes |
|---|---|
| `server.port` | `${KTOR_PORT:8120}` — 8120 is label-follower's `api` host port (see the root README's port table); the container overrides it with `--server.port=${API_PORT}`/8080 |
| `spotify.clientId` | Spotify app client ID (not secret, safe to commit) |
| `spotify.clientSecret` | `${SPOTIFY_CLIENT_SECRET}` — must be set as an env var, never hardcode a real value |
| `spotify.authUri` / `spotify.apiUri` | Spotify OAuth token endpoint and API base URL |
| `mysql.host` / `mysql.port` / `mysql.user` / `mysql.password` | host/user/password are required in production; the dev file defaults to `localhost`/`8122`/`root`/empty, matching `backend/docker-compose.yml up -d mysql` — 8122 is the block's `mysql` host port. The `-production` profile defaults the port to the fixed internal `3306` instead |
| `telemetry.service-name` | `${OTEL_SERVICE_NAME:label-follower}` — also the custom meter scope name |
| `telemetry.endpoint` | `${OTEL_EXPORTER_OTLP_ENDPOINT:}` — dev falls back to `http://localhost:4318`; **required under `production`** (boot fails without it) |
| `telemetry.console` | `${TELEMETRY_CONSOLE:false}` — dev-only console exporters; rejected under `production` |
| log level | `${LOG_LEVEL:-${LOGBACK_LOG_LEVEL:-INFO}}` — `LOGBACK_LOG_LEVEL` stays a deprecated alias |

The rest of the telemetry contract (`OTEL_*`, `SERVICE_VERSION`, `HOSTNAME`) is read from the environment untouched — see [`docs/observability.md`](docs/observability.md) §2. The production deploy must set `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_SERVICE_NAME` before the first ported image ships, and `deploy/supervisord.conf` sets `stopwaitsecs=20` to cover the SDK's 10 s shutdown flush.

The four host ports the project owns (api `8120`, web `8121`, mysql `8122`, frontend-dev `8123`) are recorded in [`.salgadinhos/ports.json`](../.salgadinhos/ports.json); `MysqlEnv.fromEnvironment()` in the migration path reads `MYSQL_PORT` with the same `8122` fallback.

## Build & maintenance

```bash
./gradlew build
./gradlew unitTest --no-daemon   # Docker-free lane
./gradlew test --no-daemon       # full suite, needs Docker
./gradlew dependencyUpdates      # CSV report in build/dependencyUpdates/report — check before bumping deps by hand
```

## Git

Remote: `git@github.com:luiznaac/label-follower.git`. Commits are lowercase, imperative/gerund (`"Persisting tracks to txt"`, `"Fixing tests"`), merged via numbered PRs. `.gitignore` ignores `*.txt` — a leftover from the pre-MySQL flat-file persistence (see "Configuration"); harmless, kept for old checkouts, no longer relevant to how the app persists data.

## Related repositories

Same author/family as [chameidor](../../chameidor/AGENTS.md) and [portfolio-2](../../portfolio-2/AGENTS.md), but architecturally the odd one out — it predates their Ktor multi-module pattern. Don't port conventions from those two here without being asked; equally, don't use this repo's structure as a template for new services in this family. The `backend/` + `frontend/` monorepo shape and the frontend stack are borrowed from [shougong](../../shougong/AGENTS.md).
