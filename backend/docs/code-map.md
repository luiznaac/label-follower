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

### `integrations/httputils` — shared HTTP wrapper
**Purpose**: the small OkHttp wrapper and response helpers every Spotify gateway uses.
**Files**: `backend/src/main/kotlin/com/rafaelfo/labelfollower/integrations/httputils/{RafaHttp,ResponseHelpers}.kt`
**Seam(s)**: **`RafaHttp`** is the single request/parse helper; changing its interface touches every Spotify gateway. It must never be imported from `usecases/` or `models/` (`LayeringTest`).

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
