-- Persistent storage for Volvo OAuth tokens, replacing the in-memory map.
-- Without this, every backend restart kicks the user out and the poller stops
-- capturing sessions until someone re-signs-in — which is exactly how we missed
-- the charge between 2026-05-27 and 2026-06-03.
--
-- Tokens are stored unencrypted at this stage. The single-tenant deployment
-- model assumes the host filesystem and database disk are encrypted at rest
-- (FileVault, LUKS, etc.); app-level encryption can be added later if we
-- multi-tenant.
CREATE TABLE oauth_token (
    user_id       TEXT PRIMARY KEY,
    vehicle_vin   TEXT,
    access_token  TEXT NOT NULL,
    refresh_token TEXT,
    expires_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
