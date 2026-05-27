-- Volvo's `updatedAt` (stored as recorded_at) only advances when the car
-- actually reports new data, so repeated polls of an idle car all share one
-- timestamp. That makes "latest snapshot" ordering by recorded_at ambiguous.
-- ingested_at records when *we* persisted the row, giving a deterministic
-- "most recently captured" order.
ALTER TABLE charging_snapshot
    ADD COLUMN ingested_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX idx_charging_snapshot_vin_ingested_at
    ON charging_snapshot (vehicle_vin, ingested_at DESC);
