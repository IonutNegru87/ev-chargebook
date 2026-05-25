# ev-chargebook

A self-hosted charging history tracker for the Volvo EX30, built on the public Volvo Energy API.

The Volvo Energy API exposes a live snapshot of charging state but no historical sessions. This project synthesises history by polling the API, detecting plug/charging transitions, and persisting one row per session plus raw snapshots.

See [EX30-Charging-History-Feasibility.md](EX30-Charging-History-Feasibility.md) for the full research write-up.

## Modules

- `shared/` — KMP module. Plays the `core:domain` role: domain models, Volvo API DTOs, `Result`/`Error`/`DataError`, the `VolvoEnergyDataSource` interface, analytics helpers. Pure Kotlin, no platform deps.
- `backend/` — Ktor (JVM) server: OAuth, polling scheduler, session detector, persistence (Postgres + Timescale), REST + SSE API. Koin wires the layers.
- `build-logic/` — Gradle convention plugins (`chargebook.kmp-domain`, `chargebook.ktor-server`). Included as a composite build so module build files stay one-liners.
- `infra/` — Flyway migrations entry point, optional Grafana dashboards.
- `docs/` — architecture notes, API quirks, ADRs.

`android/` and `frontend/` will be added later. When Android lands, the safe-call HTTP helpers in `backend/http/` get promoted to a new `:core:data` module so both clients share them.

## Architecture conventions

Patterns are pulled from the team's Android skills (data layer, error handling, Koin DI, module structure) and applied where they map to a Ktor server:

- **Data source vs repository.** Classes that wrap a single source are `*DataSource` (`KtorVolvoEnergyDataSource`, `ExposedSessionDataSource`). The term `Repository` is reserved for classes that combine multiple sources — none exist yet.
- **Typed errors.** Every data-layer call returns `Result<T, DataError.Network>` or `Result<T, DataError.Local>`. No raw exceptions cross layer boundaries.
- **DTO ↔ domain split.** `RechargeStatusDto` lives in `shared/api/`; `ChargingSnapshot` is the domain type. Mappers are extension functions in `backend/volvo/Mappers.kt`.
- **Koin per area.** One module per layer/area (`authModule`, `volvoModule`, `persistenceModule`, …) assembled in `Application.module()`. Constructor-reference `singleOf` is the default; lambda form only when a factory is needed.

## Bootstrapping

The Gradle wrapper jar is not committed yet. To generate it:

```sh
gradle wrapper --gradle-version 8.11
```

After that:

```sh
./gradlew :backend:run
```

## Local infra

```sh
docker compose up -d
```

This brings up Postgres (with the TimescaleDB extension) for the backend.

## Configuration

Backend reads its config from environment variables (see [`backend/src/main/resources/application.conf`](backend/src/main/resources/application.conf)):

| Var | Purpose |
|---|---|
| `VOLVO_CLIENT_ID` | OAuth client id from developer.volvocars.com |
| `VOLVO_CLIENT_SECRET` | OAuth client secret |
| `VOLVO_VCC_API_KEY` | vcc-api-key header value |
| `VOLVO_REDIRECT_URI` | OAuth redirect URI registered on the developer portal |
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/chargebook` |
| `DATABASE_USER` / `DATABASE_PASSWORD` | DB credentials |

## Namespace

All Kotlin code lives under `io.github.inegru.chargebook.*`.
