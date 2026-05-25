# ADR 0001 — Kotlin + Ktor for the backend

Date: 2026-05-25
Status: Accepted

## Context

The project needs a backend that performs OAuth, polls the Volvo Energy API on a coroutine-driven schedule, persists synthesised charging sessions, and serves a small REST + SSE API. A future Android app will consume the same domain types and (likely) the same backend.

## Decision

Use Kotlin + Ktor on the JVM, with a KMP `:shared` module holding the domain types and Volvo DTOs.

## Consequences

- Domain types are shared between backend, future Android app, and (optionally) a Kotlin/JS frontend — no duplicate schemas, no codegen.
- Coroutines fit the polling-loop / OAuth-refresh shape naturally.
- Single language end-to-end. The cost is heavier than Next.js for a v1, but the second client (Android) is already on the roadmap.

## Alternatives considered

- **Next.js / TypeScript backend.** Faster v1; would require a hand-written or generated TS client mirroring the Kotlin domain model later. Rejected because Android is a near-term goal.
- **Python (FastAPI).** Closest to the existing prior-art ecosystem (Home Assistant, `volvo2mqtt`) but offers no path to a shared Android client.
