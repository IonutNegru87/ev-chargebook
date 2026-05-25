# Volvo EX30 Charging History — Feasibility Report & Project Proposal

_Prepared 2026-05-25 for Ionut. Sources are listed at the bottom._

## TL;DR

Yes, the project is feasible. Volvo publishes an official **Energy API** that returns real-time charging state for the EX30 (charge level, charging power, connection status, charging type AC/DC, estimated time). **It does NOT expose historical sessions** — but you can synthesize history by polling the API on a schedule, detecting plug-in/plug-out transitions, and persisting each session yourself. This is exactly how the official Home Assistant Volvo integration works under the hood.

The remaining 2024 limitation around the EX30 has been mostly resolved: the EX30 is now supported by both the official Home Assistant integration (since HA 2025.8) and the community `volvo2mqtt` add-on, confirming the real-world feasibility of pulling EX30 data via the public API.

No public KMP/Kotlin/Java client exists yet — only Python wrappers. So your project also fills a gap in the ecosystem.

---

## 1. What Volvo's API gives you

### Energy API (v2) — the primary endpoint for this project

| Endpoint | Field(s) | EX30 support |
|---|---|---|
| `GET /energy/v2/vehicles/{vin}/recharge-status` | Composite snapshot of all energy fields | ✅ |
| `.../battery-charge-level` | State of charge (%) | ✅ |
| `.../electric-range` | Remaining range (km/mi) | ✅ |
| `.../estimated-charging-time` | Minutes to reach target SoC | ✅ |
| `.../charging-connection-status` | Connected / disconnected, AC or DC | ✅ |
| `.../charging-system-status` | Charging / idle / paused / fault | ✅ |
| `.../charging-power` (v2) | Live charging power in kW | ✅ |
| `.../target-battery-charge-level` | Target SoC % | ❌ EX30 |
| `.../charging-current-limit` | Amp limit | ❌ EX30 |

### Auxiliary APIs

- **Connected Vehicle API** — odometer, engine status, location (one-shot)
- **Location API** — current GPS position (useful to log _where_ a session happened: home vs. public charger)
- **Extended Vehicle API** — odometer, fuel level, doors/windows (less relevant here)

### What's missing — and how we compensate

| You want | API gives you | Your app fills the gap by |
|---|---|---|
| Historical session list | ❌ | Polling, detecting `charging-system-status` transitions, persisting one row per session |
| kWh delivered per session | ❌ (no totals) | Integrating power over time, or computing from ΔSoC × battery capacity (~64 kWh usable for EX30 Twin / 49 kWh Single Motor RWD) |
| Cost per session | ❌ | Apply a tariff (€/kWh by time-of-use) to computed kWh |
| Charger identity (home vs. public) | ❌ | Use Location API at session start, reverse-geocode and cluster |

---

## 2. Authentication

OAuth 2.0 Authorization Code Flow with PKCE. Concretely:

1. Sign up at <https://developer.volvocars.com/>, create an **API Application** per car/account (free).
2. You get a `client_id`, `client_secret`, and a `vcc-api-key`.
3. End user goes through Volvo ID login (one-time) → your app stores the refresh token.
4. Every request needs both `Authorization: Bearer <token>` and `vcc-api-key: <key>` headers.
5. OTP via email is required on first sign-in (same as the Volvo app).

**Scopes you need** (subset):

- `openid`
- `energy:battery_charge_level`
- `energy:recharge_status`
- `energy:state` (or per-field scopes if you prefer least-privilege)
- `conve:vehicle_relation`, `conve:fuel_status` (vehicle metadata)
- `location:read` (optional — for "where did I charge" feature)

## 3. Rate limits — and what that means for polling

- **100 requests / minute** per (user × client_id)
- **10,000 requests / day** per API key
- Exceeding either returns HTTP 429

Practical polling strategy (well within limits):

| State | Interval | ~Requests/day |
|---|---|---|
| Plugged in & charging | every 60s × ~5 calls | ~7,200 if charging 24h (rare) |
| Plugged in & idle/full | every 5 min | ~1,400 |
| Disconnected | every 30 min | ~50 |

If you ever bump the limit, the existing community projects use **multiple API keys round-robin** — Volvo allows several apps per developer account.

## 4. Existing prior art (reuse, don't reinvent)

| Project | Language | Useful for |
|---|---|---|
| [thomasddn/volvo-cars-api](https://github.com/thomasddn/volvo-cars-api) | Python | Reference for OAuth flow & endpoint coverage |
| [Home Assistant `volvo` integration](https://www.home-assistant.io/integrations/volvo) | Python | Canonical list of supported entities + polling cadence |
| [Dielee/volvo2mqtt](https://github.com/Dielee/volvo2mqtt) | Python | EX30 confirmed working; OTP flow reference |
| [volvo-cars/developer-portal-api-samples](https://github.com/volvo-cars/developer-portal-api-samples) | JS | Official OAuth2 PKCE sample code |

No Kotlin/JVM/KMP wrapper exists — you'd build the first one.

---

## 5. Recommended architecture

You said KMP-preferred. Here's how that maps cleanly onto this problem:

```
┌────────────────────────────────────────────────────────────┐
│ Web UI (Compose for Web / React) — dashboard, history list │
└────────────────────────────────────────────────────────────┘
                          │ REST/GraphQL
┌─────────────────────────▼──────────────────────────────────┐
│ Backend (Ktor, JVM)                                        │
│  • OAuth dance + token vault                               │
│  • Poller service (coroutines)                             │
│  • Session detector (state machine on charging-system-status)│
│  • REST API for the frontend                               │
└─────────────────────────┬──────────────────────────────────┘
                          │
                ┌─────────▼─────────┐         ┌──────────────┐
                │ Postgres + Timescale│◀──────│ Volvo API    │
                │  • sessions table   │       │ (energy/v2)  │
                │  • snapshots hypertable     └──────────────┘
                └─────────────────────┘
```

### Why Kotlin + Ktor on the backend (instead of Next.js)

- You can put domain types (`ChargingSession`, `Snapshot`, `ChargingSystemStatus`) in a **shared KMP module** consumed by:
  - the Ktor backend
  - the web frontend (Compose for Web / Kotlin/JS, or React with kotlinx-serialization-generated TS types)
  - a future Android app
- Coroutines + Ktor client = clean polling loop, OAuth refresh, retries with exponential backoff.
- Single language end-to-end — no context switching when you add the Android app later.

### Alternative if you'd rather skip KMP for v1

Backend in **Kotlin + Ktor**, frontend in **plain React + TypeScript** (generate TS types from Kotlin DTOs with `kxs-ts-gen` or hand-write). Less ambitious, ships faster. KMP can come in later when you start the Android app.

I'll scaffold with the **KMP-shared-domain + Ktor backend + React frontend** structure since it gives you both options.

---

## 6. Proposed dev folder structure

```
ex30-charging-tracker/
├── README.md
├── LICENSE
├── .editorconfig
├── .gitignore
├── docker-compose.yml          # postgres + timescale + the app
├── gradle/
├── settings.gradle.kts         # KMP multi-module
├── build.gradle.kts            # root build
│
├── shared/                     # KMP shared module — pure Kotlin
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/com/ex30tracker/shared/
│       ├── model/
│       │   ├── ChargingSession.kt
│       │   ├── ChargingSnapshot.kt
│       │   ├── ChargingSystemStatus.kt
│       │   ├── ChargingConnectionStatus.kt
│       │   └── Vehicle.kt
│       ├── api/                # DTOs that mirror Volvo Energy API responses
│       │   ├── RechargeStatusDto.kt
│       │   ├── BatteryChargeLevelDto.kt
│       │   └── ...
│       └── analytics/
│           ├── SessionAggregates.kt   # kWh, avg power, cost
│           └── EfficiencyCalc.kt
│
├── backend/                    # Ktor server
│   ├── build.gradle.kts
│   ├── Dockerfile
│   └── src/main/kotlin/com/ex30tracker/backend/
│       ├── Application.kt
│       ├── config/
│       │   └── Env.kt
│       ├── auth/
│       │   ├── VolvoOAuthClient.kt    # PKCE + token refresh
│       │   ├── TokenStore.kt          # encrypted at rest
│       │   └── AuthRoutes.kt          # /auth/start, /auth/callback
│       ├── volvo/
│       │   ├── VolvoApiClient.kt      # Ktor client wrapping Energy API
│       │   ├── EnergyEndpoints.kt
│       │   └── RateLimiter.kt
│       ├── poller/
│       │   ├── PollingScheduler.kt    # adaptive cadence (charging/idle/disconnected)
│       │   └── SessionDetector.kt     # state machine, emits SessionStart/SessionEnd
│       ├── persistence/
│       │   ├── Database.kt            # Exposed or jOOQ
│       │   ├── SessionRepository.kt
│       │   ├── SnapshotRepository.kt
│       │   └── migrations/            # Flyway: V1__init.sql, V2__timescale.sql
│       ├── analytics/
│       │   └── AnalyticsService.kt    # monthly totals, cost, efficiency
│       └── routes/
│           ├── SessionRoutes.kt       # GET /api/sessions, /api/sessions/{id}
│           ├── LiveRoutes.kt          # SSE /api/live (current charging state)
│           └── AnalyticsRoutes.kt     # GET /api/analytics/monthly etc.
│
├── frontend/                   # web UI — React + Vite + TypeScript
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── api/                # generated or hand-written client
│       ├── pages/
│       │   ├── Dashboard.tsx       # live state + last session
│       │   ├── History.tsx         # session list, filters, export CSV
│       │   ├── Analytics.tsx       # monthly kWh, €, range trends
│       │   └── Settings.tsx        # tariff, home location, API key mgmt
│       ├── components/
│       │   ├── ChargingCard.tsx
│       │   ├── SessionTable.tsx
│       │   └── charts/
│       └── lib/
│
├── android/                    # placeholder — added later in Claude Code
│   └── README.md               # "see KMP shared module; bring in Compose Multiplatform"
│
├── infra/
│   ├── flyway/
│   └── grafana/                # optional: prebuilt dashboards for raw snapshots
│
└── docs/
    ├── architecture.md
    ├── volvo-api-notes.md       # everything we learned about quirks & EX30 limits
    ├── data-model.md
    └── adr/
        ├── 0001-kotlin-ktor-backend.md
        └── 0002-timescale-for-snapshots.md
```

### Data model sketch

```sql
-- One row per detected charging session
CREATE TABLE charging_session (
  id              UUID PRIMARY KEY,
  vehicle_vin     TEXT NOT NULL,
  started_at      TIMESTAMPTZ NOT NULL,
  ended_at        TIMESTAMPTZ,
  start_soc_pct   INT,
  end_soc_pct     INT,
  energy_kwh      NUMERIC(6,3),       -- computed from snapshots
  avg_power_kw    NUMERIC(6,3),
  peak_power_kw   NUMERIC(6,3),
  connection_type TEXT,                -- AC / DC
  location_lat    DOUBLE PRECISION,
  location_lon    DOUBLE PRECISION,
  location_label  TEXT,                -- "Home", "Ionity Frankfurt", ...
  tariff_eur_kwh  NUMERIC(5,4),
  cost_eur        NUMERIC(8,2)
);

-- Raw polled snapshots — Timescale hypertable on (recorded_at)
CREATE TABLE charging_snapshot (
  recorded_at        TIMESTAMPTZ NOT NULL,
  session_id         UUID REFERENCES charging_session(id),
  vehicle_vin        TEXT NOT NULL,
  soc_pct            INT,
  power_kw           NUMERIC(6,3),
  range_km           INT,
  estimated_minutes  INT,
  charging_status    TEXT,
  connection_status  TEXT
);
SELECT create_hypertable('charging_snapshot', 'recorded_at');
```

### Session detection (the only non-obvious bit)

A small state machine consuming the polling stream:

```
DISCONNECTED ──(connection_status=CONNECTED)──► IDLE_PLUGGED
IDLE_PLUGGED ──(system_status=CHARGING)──► CHARGING  [emit SessionStart]
CHARGING     ──(system_status≠CHARGING for N polls)──► IDLE_PLUGGED  [emit SessionEnd if session was non-trivial]
ANY          ──(connection_status=DISCONNECTED)──► DISCONNECTED  [emit SessionEnd]
```

`energy_kwh` is the trapezoidal integral of `power_kw × Δt` across the session's snapshots — falling back to `(end_soc − start_soc) × battery_capacity_kwh / 100` if power readings are sparse.

---

## 7. Suggested v1 milestones

1. **OAuth + first read** — sign in, fetch recharge status, dump to console.
2. **Snapshot persistence** — polling loop writing to Postgres.
3. **Session detector + history list** — `/sessions` endpoint + bare React table.
4. **Live view** — SSE channel with current power/SoC/ETA.
5. **Analytics** — monthly kWh/cost, simple charts.
6. **Polish** — tariff config, home-location detection, CSV export.
7. **Android (later, in Claude Code)** — Compose Multiplatform UI consuming the same backend, with offline-first cache.

Estimated effort to v1 (steps 1–5): a few focused weekends if you already know Kotlin/Ktor; longer if KMP is new.

---

## 8. Risks & open questions

- **OAuth refresh expires on "custom application credentials"** — Volvo limits the grant period. Your backend must surface re-auth prompts gracefully. (Home Assistant works around this by using Nabu Casa as a proxy issuer.)
- **API quotas** — 10k/day is plenty for one car, but if you ever multi-tenant this, you'll need a key per user.
- **EX30 firmware updates** — the Geely-based platform has changed what fields work over the past year. Plan for graceful degradation per field.
- **`charging-power` field semantics** — community has noted `FAULT` values when the charger pauses. Treat as nullable.

---

## Sources

- [Energy API overview – Volvo Cars Developer Portal](https://developer.volvocars.com/apis/energy/v1/overview/)
- [Energy API v2 – Volvo Cars Developer Portal](https://developer.volvocars.com/apis/energy/v2/overview/)
- [Recharge status endpoint](https://developer.volvocars.com/apis/energy/v1/endpoints/recharge-status/)
- [Charging system status endpoint](https://developer.volvocars.com/apis/energy/v1/endpoints/charging-system-status/)
- [Charging connection status endpoint](https://developer.volvocars.com/apis/energy/v1/endpoints/charging-connection-status/)
- [Battery charge level endpoint](https://developer.volvocars.com/apis/energy/v1/endpoints/battery-charge-level/)
- [Estimated charging time endpoint](https://www.developer.volvocars.com/apis/energy/v1/endpoints/estimated-charging-time/)
- [Authorisation guide](https://developer.volvocars.com/apis/docs/authorisation/)
- [API Rate Limits announcement](https://developer.volvocars.com/news/api-rate-limits/)
- [Introducing the next version of Energy API](https://developer.volvocars.com/news/energy-api-updates/)
- [Home Assistant Volvo integration](https://www.home-assistant.io/integrations/volvo)
- [thomasddn/volvo-cars-api (Python client)](https://github.com/thomasddn/volvo-cars-api)
- [thomasddn/ha-volvo-cars (archived, reference)](https://github.com/thomasddn/ha-volvo-cars)
- [Dielee/volvo2mqtt (EX30 confirmed working)](https://github.com/Dielee/volvo2mqtt)
- [volvo-cars/developer-portal-api-samples (official OAuth2 sample)](https://github.com/volvo-cars/developer-portal-api-samples)
- [EX30 API support issue (Geely platform note)](https://github.com/volvo-cars/developer-portal-api-samples/issues/16)
