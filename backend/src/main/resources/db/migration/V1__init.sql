CREATE TABLE charging_session (
    id              UUID PRIMARY KEY,
    vehicle_vin     TEXT NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL,
    ended_at        TIMESTAMPTZ,
    start_soc_pct   INT,
    end_soc_pct     INT,
    energy_kwh      NUMERIC(6,3),
    avg_power_kw    NUMERIC(6,3),
    peak_power_kw   NUMERIC(6,3),
    connection_type TEXT,
    location_lat    DOUBLE PRECISION,
    location_lon    DOUBLE PRECISION,
    location_label  TEXT,
    tariff_eur_kwh  NUMERIC(5,4),
    cost_eur        NUMERIC(8,2)
);

CREATE INDEX idx_charging_session_vin_started_at
    ON charging_session (vehicle_vin, started_at DESC);

CREATE TABLE charging_snapshot (
    recorded_at        TIMESTAMPTZ NOT NULL,
    session_id         UUID REFERENCES charging_session(id) ON DELETE SET NULL,
    vehicle_vin        TEXT NOT NULL,
    soc_pct            INT,
    power_kw           NUMERIC(6,3),
    range_km           INT,
    estimated_minutes  INT,
    charging_status    TEXT,
    connection_status  TEXT
);

CREATE INDEX idx_charging_snapshot_vin_recorded_at
    ON charging_snapshot (vehicle_vin, recorded_at DESC);

CREATE INDEX idx_charging_snapshot_session
    ON charging_snapshot (session_id);
