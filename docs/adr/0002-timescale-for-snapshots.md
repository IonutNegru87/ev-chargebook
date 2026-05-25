# ADR 0002 — TimescaleDB hypertable for snapshots

Date: 2026-05-25
Status: Accepted

## Context

`charging_snapshot` grows monotonically — one row per API poll, roughly every 1–5 minutes when plugged in. Within a year a single car can produce hundreds of thousands of rows; multi-tenant deployments would push this much higher. The dominant query is "snapshots for vehicle V between t0 and t1", which is exactly what TimescaleDB optimises with its time-partitioned hypertables.

## Decision

Store `charging_snapshot` as a TimescaleDB hypertable partitioned on `recorded_at`. Keep `charging_session` as a regular Postgres table — it's bounded and small.

## Consequences

- Fast range queries; clean retention policies later if we want to age out raw snapshots while keeping aggregated sessions.
- The Docker image is `timescale/timescaledb` rather than vanilla `postgres`. Managed-Postgres providers without Timescale support won't work out of the box — Supabase, RDS w/o the extension, etc.

## Alternatives considered

- **Vanilla Postgres with a BRIN index on `recorded_at`.** Simpler ops; acceptable single-tenant performance for the first year. Reconsider if Timescale becomes a deployment blocker.
