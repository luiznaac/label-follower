# Code map

Module → key files → seam index for the `backend/` lane, plus the app entry points. This is the family's oldest backend: a **single Gradle module** using Spring MVC (not the multi-module Ktor split of chameidor/portfolio-2), so each package cluster is a module. Ports live in `usecases/`, vendor adapters in `integrations/`, and `LayeringTest` (Konsist) enforces the separation. Vocabulary: **module**, **interface**, **seam**, **adapter**.

## Modules

### `api` — Spring MVC controllers
**Purpose**: the HTTP boundary — thin `@RestController`s that extract request data and delegate straight to a usecase.
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/api/ConsolidatorController.kt`, `api/IntrospectController.kt`, `api/TrackController.kt`, `api/AuthController.kt`
**Seam(s)**:
- **Controller → usecase** (`ConsolidatorController.kt`, `IntrospectController.kt`, `TrackController.kt`) — calls `Consolidator.introspectAllLabelsAndNotify()`, or `TrackFinder.findBy(isrc)` followed by `LabelIntrospector.introspectFrom(...)` / `.discoverNewTracksFrom(...)`. Keep business logic out of the method body; only path-variable and header extraction belong here.
- **Auth boundary** (`AuthController.kt`) — `GET /auth/spotify/status`, `POST /auth/spotify/exchange` and `DELETE /auth/spotify`; the code exchange is server-side, so the backend owns the refresh token and the frontend never holds one.

### `usecases` — business logic and ports
**Purpose**: the orchestration services plus the three port interfaces the integration layer implements.
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/usecases/{Consolidator,LabelIntrospector,TrackFinder}.kt`, `usecases/{OurInfoGateway,ExternalInfoGateway,UserInfoGateway}.kt`
**Seam(s)**:
- **`OurInfoGateway`** (`usecases/OurInfoGateway.kt`) — persisted label/track data (`getTracksFrom`, `saveTracks`, `getLabelBy`, `getLabels`); implemented by `OurInfoGatewayImpl`. It exposes only `Label`/`Track`, never Exposed types.
- **`ExternalInfoGateway`** (`usecases/ExternalInfoGateway.kt`) — third-party lookups (`getLabel`, `findTrackBy`, `getTracksFrom`); implemented by `SpotifyGateway`. No `Spotify*` type may appear in `usecases/`.
- **`UserInfoGateway`** (`usecases/UserInfoGateway.kt`) — playlist creation (`createPlaylistWith`); implemented by `SpotifyUserPlaylistGateway`.
- **Orchestration order** (`Consolidator.kt`) — `getLabels()` → `discoverNewTracksFrom(...)` per label → `createPlaylistWith(...)` only for non-empty results; don't reverse it or notify before discovery finishes.

### `usecases/telemetry` — telemetry contract constants
**Purpose**: the token-free names every telemetry wiring and test shares — semconv attribute keys, the bounded `error.type` code map and the metric catalog (names, units, buckets, attribute allowlists).
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/usecases/telemetry/{TelemetryAttributes,MetricCatalog,TelemetryLogging}.kt`
**Seam(s)**: **catalog ↔ wiring** — `TelemetryViews` and the contract tests read the same constants, so a series cannot drift between the SDK configuration and its assertion. The OTel **API** may be imported here; SDK, exporters and instrumentation may not.

### `models` — domain data
**Purpose**: plain Kotlin data classes with no framework annotations.
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/models/Label.kt`, `models/Track.kt`
**Seam(s)**: **`Label.matches(...)`** (`models/Label.kt`) is the label-identity rule — name equality plus copyright overlap (case-insensitive) — shared by the Spotify adapter and persistence; changing it affects both sides. `LayeringTest` keeps `models/` free of Spring, Exposed, OkHttp and Gson.

### `integrations/database` — Exposed adapter
**Purpose**: implements `OurInfoGateway` against MySQL via Exposed.
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/integrations/database/{OurInfoGatewayImpl,AllTables,LabelTable,LabelTrackTable,TrackTable,SpotifyAccountTable}.kt`
**Seam(s)**:
- **Adapter seam** (`OurInfoGatewayImpl.kt`) — `transaction { … }` stays inside this package; `LabelTable`/`LabelTrackTable`/`TrackTable` map the schema, `SpotifyAccountTable` stores the backend's Spotify login.
- **Schema source of truth** (`AllTables.kt`) — `allTables` feeds `MigrationScripts` and `MigrationSchemaTest`; a new table must be registered here, and `V1__init.sql` must match.

### `integrations/spotify` — Spotify adapters
**Purpose**: implements `ExternalInfoGateway` and `UserInfoGateway` against the real Spotify API.
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/integrations/spotify/{SpotifyGateway,SpotifyAlbumGateway,SpotifyTrackGateway,SpotifyLabelGateway,SpotifyUserPlaylistGateway,SpotifyAuth,SpotifyUserAuth,SpotifyConfig}.kt`; `integrations/spotify/models/*`, `integrations/spotify/responses/*`
**Seam(s)**:
- **`SpotifyGateway` → `ExternalInfoGateway`** (`SpotifyGateway.kt`) — composes the album/track/label gateways; Spotify JSON models stay inside this package.
- **`SpotifyUserPlaylistGateway` → `UserInfoGateway`** (`SpotifyUserPlaylistGateway.kt`) — creates the playlist in chunks and names it from the label; chunk size and name format are adapter-local.
- **`SpotifyAuth` / `SpotifyUserAuth`** — client-credentials vs user-token exchange and refresh; change both together when the auth flow moves.

### `config/telemetry` — OpenTelemetry wiring
**Purpose**: the composition-root seam for logs and metrics. It builds the OTel SDK from `TelemetrySettings`, installs the Logback appender, pins the metric views, redacts MDC, renders the prod JSON record, binds `request.id` and emits the single 4xx/5xx boundary record.
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/config/telemetry/{Telemetry,TelemetrySettings,TelemetryExporters,TelemetryConfig,TelemetryResource,TelemetryViews,TelemetryJsonProviders,RedactingMdcFilter,RequestId,RequestTelemetryFilter,BoundaryTelemetry,ServletTelemetryConfig}.kt`; `backend/src/main/resources/logback-spring.xml`
**Seam(s)**:
- **SDK lifecycle** (`TelemetryConfig.kt`) — resolves `telemetry.*` from the Spring environment, builds the SDK once, installs the Logback appender and flushes in `@PreDestroy`. No `TracerProvider` and no trace exporter; a missing endpoint under `production` fails at boot.
- **Request correlation** (`RequestTelemetryFilter.kt`, `BoundaryTelemetry.kt`) — the filter runs at `HIGHEST_PRECEDENCE` (the framework instrumentation filter follows one rung behind), resolves/echoes `X-Request-ID`, binds `request.id` in MDC and owns the one boundary record; the resolver runs first in the exception chain and only stores the failure for the filter.
- **Redaction** (`RedactingMdcFilter.kt`) — one Logback `TurboFilter` ahead of both appenders, so the OTLP record and the stdout line cannot diverge.

### `integrations/httputils` — shared HTTP wrapper
**Purpose**: the small OkHttp wrapper and response helpers every Spotify gateway uses.
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/integrations/httputils/{RafaHttp,ResponseHelpers,OkHttpConfig,RequestIdInterceptor}.kt`
**Seam(s)**: **`RafaHttp`** is the single request/parse helper; changing its interface touches every Spotify gateway. It must never be imported from `usecases/` or `models/` (`LayeringTest`). **`OkHttpConfig`** owns the one shared `Call.Factory`: the OkHttp instrumentation records `http.client.request.duration` and `RequestIdInterceptor` propagates `request.id` from MDC.

### `application` and `config` — boot and infrastructure
**Purpose**: the Spring Boot entry, the profile/config beans, and the Flyway migration tooling.
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/application/Boot.kt`; `config/{Config,DatabaseConfig}.kt`; `config/migration/{MigrationScripts,Migrator,MysqlEnv}.kt`; `profiles/Development.kt`
**Seam(s)**:
- **Boot** (`application/Boot.kt`) — `@ComponentScan("com.rafaelfo.labelfollower")`; it must match `mainClass` in `build.gradle.kts`.
- **Migration seam** (`config/migration/MigrationScripts.kt` + `AllTables.kt`) — regenerates `V*.sql`; `Migrator.main()` is a standalone process run by `deploy/entrypoint.sh` before the app, and aborts the container on failure.
- **Connection seam** (`config/DatabaseConfig.kt`) — `mysqlJdbcUrl(...)` is shared with `Migrator`, keeping Exposed and Flyway on one URL.

### Build, tests and deploy
**Purpose**: the Gradle/detekt baseline, the layering guard, and the packaging story.
**Files**: `backend/build.gradle.kts`, `backend/settings.gradle.kts`, `backend/config/detekt/{config,format}.yml`; `backend/src/test/kotlin/com/rafaelfo/labelfollower/architecture/LayeringTest.kt`, `config/migration/MigrationSchemaTest.kt`, `usecases/{LabelIntrospectorTest,TrackFinderTest}.kt`, `integrations/spotify/*Test.kt`, `models/LabelTest.kt`, `integrations/httputils/RafaHttpTest.kt`; `deploy/{entrypoint.sh,nginx.conf.template,supervisord.conf}`, `docker-compose.yml`
**Seam(s)**:
- **Layering guard** (`architecture/LayeringTest.kt`) — Konsist fails the build if `usecases/` imports `integrations/` or Spotify types, or `models/` imports framework/vendor packages; this is the compensation for not having one Gradle module per layer.
- **Schema guard** (`config/migration/MigrationSchemaTest.kt`) — asserts Exposed's `allTables` needs no extra migration against the migrated test database.
- **Deploy entry** (`deploy/entrypoint.sh`) — migrate, then start the app; `nginx.conf.template` serves the built SPA and proxies `/api`.

## Entry points

- `backend/src/main/kotlin/com/rafaelfo/labelfollower/application/Boot.kt` — Spring Boot `main()`; component-scans the whole package.
- `backend/src/main/kotlin/com/rafaelfo/labelfollower/config/migration/Migrator.kt` — standalone `main()`; Flyway `migrate()`, run by `deploy/entrypoint.sh`.
- `backend/src/main/kotlin/com/rafaelfo/labelfollower/config/migration/MigrationScripts.kt` — standalone `main()`; the Exposed migration-script generator (`-Pname=…`).
- `backend/src/main/kotlin/com/rafaelfo/labelfollower/integrations/database/AllTables.kt` — `allTables`, consumed by both the generator and `MigrationSchemaTest`.
- `backend/src/main/resources/db/migration/V1__init.sql` — the applied schema; must match `AllTables`.
