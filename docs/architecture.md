# Architecture

```
┌────────────────────────────────────────────────────────────┐
│ Web UI (added later) — dashboard, history list             │
└────────────────────────────────────────────────────────────┘
                          │ REST / SSE
┌─────────────────────────▼──────────────────────────────────┐
│ Backend (Ktor, JVM)                                        │
│  • OAuth dance + token vault                               │
│  • Poller service (coroutines)                             │
│  • Session detector (state machine on charging-system-status)│
│  • REST + SSE API                                          │
└─────────────────────────┬──────────────────────────────────┘
                          │
                ┌─────────▼─────────┐         ┌──────────────┐
                │ Postgres + Timescale│◀──────│ Volvo API    │
                │  • charging_session │       │ (energy/v2)  │
                │  • charging_snapshot │      └──────────────┘
                └─────────────────────┘
```

Domain types live in `:shared` (KMP) so the same `ChargingSession` / `ChargingSnapshot` definitions are reused by the backend now and by Android / a Kotlin frontend later.
