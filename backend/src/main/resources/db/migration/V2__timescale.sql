-- Requires the TimescaleDB extension to be available (provided by the
-- timescale/timescaledb Docker image used in docker-compose.yml).
CREATE EXTENSION IF NOT EXISTS timescaledb;

SELECT create_hypertable(
    'charging_snapshot',
    'recorded_at',
    if_not_exists => TRUE,
    migrate_data => TRUE
);
