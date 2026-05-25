# Volvo Energy API notes

Operational quirks worth knowing — collected from the feasibility report and from Home Assistant / `volvo2mqtt` issue trackers. Update this file as we hit new edge cases.

## Auth

- OAuth 2.0 Authorization Code Flow with PKCE.
- Every API call needs both `Authorization: Bearer <token>` and `vcc-api-key: <key>` headers.
- First sign-in requires an OTP via email — same flow as the Volvo app.
- Refresh tokens on "custom application credentials" have a limited grant period; surface re-auth prompts gracefully.

## EX30-specific field support (Energy API v2)

Confirmed by hitting `GET /energy/v2/vehicles/{vin}/state` against a real EX30 in May 2026. The v2 envelope returns each field as `{ status, value, ... }` where `status` is `"OK"` or `"ERROR"`; ERROR responses include a `code` (`PROPERTY_NOT_SUPPORTED` for things the car never has, `PROPERTY_NOT_FOUND` for things temporarily unavailable).

| Field | EX30 |
|---|---|
| `batteryChargeLevel` | ✅ OK |
| `electricRange` | ✅ OK |
| `chargerConnectionStatus` | ✅ OK |
| `chargingStatus` | ✅ OK |
| `chargingType` | ✅ OK (NONE / AC / DC) |
| `chargerPowerStatus` | ✅ OK |
| `estimatedChargingTimeToTargetBatteryChargeLevel` | ✅ OK |
| `targetBatteryChargeLevel` | ✅ OK — works in v2, contradicting the v1 feasibility doc |
| `chargingPower` | ⚠️ `PROPERTY_NOT_FOUND` while disconnected; should populate while charging |
| `chargingCurrentLimit` | ❌ `PROPERTY_NOT_SUPPORTED` |

Plan for graceful degradation per field — treat ERROR responses as "field absent for this sample" rather than as hard errors.

## Rate limits

- 100 req/min per (user × client_id).
- 10,000 req/day per API key.
- Round-robin across multiple API keys if we ever multi-tenant.

## Synthesised history

Volvo does not expose historical sessions. We rebuild them by polling and detecting `charging-system-status` transitions — see [`SessionDetector`](../backend/src/main/kotlin/io/github/inegru/chargebook/backend/poller/SessionDetector.kt).
