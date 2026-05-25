# Data model

Two tables, one hypertable. See [`V1__init.sql`](../backend/src/main/resources/db/migration/V1__init.sql) and [`V2__timescale.sql`](../backend/src/main/resources/db/migration/V2__timescale.sql) for the authoritative schema.

## `charging_session`

One row per detected plug-in-to-plug-out cycle where charging was observed. Aggregates (`energy_kwh`, `avg_power_kw`, `peak_power_kw`, `cost_eur`) are derived from snapshots and may be recomputed if our analytics logic changes.

## `charging_snapshot`

Raw polled samples — one row per API hit. Timescale hypertable on `recorded_at` to keep the time-series queries fast as data grows. We deliberately store everything verbatim so sessions can be re-derived.

## Energy computation

Primary: trapezoidal integral of `power_kw × Δt` across the session's snapshots.

Fallback (when power samples are sparse or missing): `(end_soc − start_soc) × battery_capacity_kwh / 100`. EX30 capacities are in [`EfficiencyCalc`](../shared/src/commonMain/kotlin/io/github/inegru/chargebook/shared/analytics/EfficiencyCalc.kt) — ~64 kWh usable for the Twin, ~49 kWh for the Single Motor RWD.
