# Volvo Energy API notes

Operational quirks worth knowing — collected from the feasibility report and from Home Assistant / `volvo2mqtt` issue trackers. Update this file as we hit new edge cases.

## Auth

- OAuth 2.0 Authorization Code Flow with PKCE.
- Every API call needs both `Authorization: Bearer <token>` and `vcc-api-key: <key>` headers.
- First sign-in requires an OTP via email — same flow as the Volvo app.
- Refresh tokens on "custom application credentials" have a limited grant period; surface re-auth prompts gracefully.

## EX30-specific gaps

| Endpoint | EX30 |
|---|---|
| `target-battery-charge-level` | ❌ unsupported |
| `charging-current-limit` | ❌ unsupported |
| `charging-power` | ✅ but may return `FAULT` mid-session |

Plan for graceful degradation per field — Geely-platform firmware updates have changed what's available over the past year.

## Rate limits

- 100 req/min per (user × client_id).
- 10,000 req/day per API key.
- Round-robin across multiple API keys if we ever multi-tenant.

## Synthesised history

Volvo does not expose historical sessions. We rebuild them by polling and detecting `charging-system-status` transitions — see [`SessionDetector`](../backend/src/main/kotlin/io/github/inegru/chargebook/backend/poller/SessionDetector.kt).
