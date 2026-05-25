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

## Running

```sh
docker compose up -d            # Postgres + TimescaleDB
set -a; source .env; set +a     # load Volvo credentials into the shell
./gradlew :backend:run
```

The Gradle wrapper is committed — no system-wide Gradle install needed. The build targets JDK 17.

## Trying the OAuth flow

Once the server is up:

1. Open <http://localhost:8080/auth/start> in a browser.
2. Sign in with your Volvo ID. First sign-in for a given account requires the OTP that Volvo emails you.
3. After consent, Volvo redirects back to `/auth/callback`. The page should say "Signed in".
4. Hit <http://localhost:8080/api/snapshot/me> — it picks the first VIN on your account, calls `GET /energy/v2/vehicles/{vin}/recharge-status`, and returns the mapped [`ChargingSnapshot`](shared/src/commonMain/kotlin/io/github/inegru/chargebook/shared/model/ChargingSnapshot.kt) as JSON.

Endpoints today:

| Path | Status |
|---|---|
| `GET /auth/start` | Redirects to Volvo authorize URL with PKCE |
| `GET /auth/callback` | Exchanges code for tokens, stores them in memory |
| `GET /api/vehicles` | Lists VINs the authenticated user has access to |
| `GET /api/snapshot/me` | Current charging snapshot for the first VIN |
| `GET /api/sessions` | Stub — returns `[]` (Exposed data source still `TODO()`) |
| `GET /api/analytics/monthly` | Stub — 501 |
| `GET /api/live` | SSE stub |

Token storage is in-memory, so signing in is gone after a server restart. Persisting tokens (encrypted) and starting the polling loop are the next milestones.

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
