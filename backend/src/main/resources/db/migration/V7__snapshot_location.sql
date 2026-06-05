-- Per-snapshot location plus reverse-geocoded label. Lets the dashboard show
-- "where is the car right now" continuously, not just during charging sessions.
-- Redundant when the car sits parked (every poll stores the same lat/lon) but
-- the storage cost is negligible and it keeps querying simple.
ALTER TABLE charging_snapshot
    ADD COLUMN location_lat   DOUBLE PRECISION,
    ADD COLUMN location_lon   DOUBLE PRECISION,
    ADD COLUMN location_label TEXT;
