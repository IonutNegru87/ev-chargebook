# ev-chargebook

A self-hosted charging history tracker for the Volvo EX30, built on the public Volvo Energy API.

The Volvo Energy API exposes a live snapshot of charging state but no historical sessions. This project synthesises history by polling the API, detecting plug/charging transitions, and persisting one row per session plus raw snapshots.

See [EX30-Charging-History-Feasibility.md](EX30-Charging-History-Feasibility.md) for the full research write-up.

## Modules

- `shared/` — KMP module (JVM + WasmJs targets). Plays the `core:domain` role: domain models, Volvo API DTOs, `Result`/`Error`/`DataError`, the `VolvoEnergyDataSource` interface, analytics helpers. Pure Kotlin, no platform deps.
- `backend/` — Ktor (JVM) server: OAuth, polling scheduler, session detector, persistence (Postgres + Timescale), REST + SSE API. Koin wires the layers.
- `web/` — Compose Multiplatform Web app (WasmJs target). Consumes `:shared` types directly and calls the backend HTTP / SSE API. Three tabs at <http://localhost:8081>: **Dashboard** (latest snapshot from `/api/snapshot/latest`, live-updating via `/api/live` SSE), **Sessions** (history list; click a session to drill into its snapshot timeline and edit tariff / solar via `PATCH /api/sessions/{id}`), **Analytics** (monthly totals with energy bars). Run with `./gradlew :web:wasmJsBrowserDevelopmentRun`.

> The dev server bundles once at startup and does **not** auto-rebuild. After changing anything in `web/` or `shared/`, restart it and hard-refresh the browser (Cmd+Shift+R).
- `build-logic/` — Gradle convention plugins (`chargebook.kmp-domain`, `chargebook.ktor-server`, `chargebook.cmp-web`). Included as a composite build so module build files stay one-liners.
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
4. Hit <http://localhost:8080/api/snapshot/me> — it picks the first VIN on your account, calls `GET /energy/v2/vehicles/{vin}/state`, and returns the mapped [`ChargingSnapshot`](shared/src/commonMain/kotlin/io/github/inegru/chargebook/shared/model/ChargingSnapshot.kt) as JSON.

Endpoints today:

| Path | Status |
|---|---|
| `GET /auth/start` | Redirects to Volvo authorize URL with PKCE |
| `GET /auth/callback` | Exchanges code for tokens, stores them in memory |
| `GET /api/vehicles` | Lists VINs the authenticated user has access to |
| `GET /api/snapshot/me` | Live snapshot for the first VIN (calls Volvo on every request) |
| `GET /api/snapshot/latest` | Most recent **persisted** snapshot — written by the polling loop |
| `GET /api/location/me` | Current GPS position via Volvo's Location API (`{lat, lon}`). The same point is also attached to every persisted snapshot, with a reverse-geocoded label from Nominatim (cached). |
| `GET /api/sessions` | List of detected charging sessions. `?vin=`, `?since=<iso>`, `?limit=` |
| `GET /api/sessions/{id}` | One session plus all its persisted snapshots |
| `PATCH /api/sessions/{id}` | Override tariff + solar contribution for a single session (see [Pricing](#pricing)) |
| `GET /api/sessions.csv` | CSV export of all sessions (`?vin=` optional) |
| `GET /api/analytics/monthly` | Aggregated by calendar month in the system timezone. `?vin=`, `?from=YYYY-MM-DD`, `?to=YYYY-MM-DD`. Returns sessions, energy_kwh, solar_kwh, billable_kwh, cost_eur per month. |
| `GET /api/live` | SSE stream of every newly persisted snapshot (event name `snapshot`, payload is the JSON `ChargingSnapshot`). The bus replays the latest known snapshot on connect. |
| `GET /health` | `{"status":"ok","db":"ok","auth":"ok"}` when both Postgres is reachable and an OAuth token is on file. Returns 503 otherwise. Single-curl liveness/readiness for monitoring. |

The poller starts on app boot. While unauthenticated it parks for 1 minute at a time; once you sign in via `/auth/start`, the next tick picks it up. Each polled snapshot is fed through [`SessionDetector`](backend/src/main/kotlin/io/github/inegru/chargebook/backend/poller/SessionDetector.kt) — a tiny state machine that emits `SessionStart` / `SessionEnd` events based on transitions in `chargingStatus` and `chargerConnectionStatus`. On start, a row is inserted into `charging_session`; subsequent snapshots get tagged with the session id; on end, the row is closed out with aggregates from [`SessionAggregates`](shared/src/commonMain/kotlin/io/github/inegru/chargebook/shared/analytics/SessionAggregates.kt) (kWh from trapezoidal integration of power, peak/avg power, end SoC). If the server restarts mid-session, the poller resumes the open `ended_at IS NULL` row instead of opening a duplicate.

Polling cadence comes from [`PollingScheduler`](backend/src/main/kotlin/io/github/inegru/chargebook/backend/poller/PollingScheduler.kt) — 60s while charging, 5min while plugged-but-idle, 30min while disconnected.

Token storage is still in-memory, so signing in is gone after a server restart. Token persistence is the next operational fix.

## Pricing

A session's cost is `(energyKwh − solarKwh) × tariffEurPerKwh`. The flow:

1. **Default tariff at session close.** When the session detector closes a session, the backend applies `DEFAULT_TARIFF_EUR_PER_KWH` from `.env` (typically your home tariff) and computes an initial `cost_eur`. Leave the env var unset to skip auto-pricing entirely (tariff and cost stay null until you set them by hand).
2. **Per-session override** via `PATCH /api/sessions/{id}` with a JSON body:
   ```json
   { "tariffEurPerKwh": 0.45, "solarKwh": 2.5 }
   ```
   Use this when the session happened at a public charger with a different price, or to record how much energy came from home solar so it doesn't count toward cost.
3. **Solar as a percentage.** Instead of `solarKwh`, you can send `solarPct` (0–100) and the backend converts against the session's `energyKwh` before storing — useful when your inverter reports a percentage rather than absolute kWh.
   ```json
   { "solarPct": 35 }
   ```
4. Every PATCH recomputes `cost_eur`. If `solarKwh > energyKwh` (overshoot from user input), the billable portion is clamped at 0.

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
