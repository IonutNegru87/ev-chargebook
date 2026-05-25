-- Adds support for solar-supplied energy on a session: the portion of the
-- session's `energy_kwh` that came from a free source (e.g. home PV) and so
-- shouldn't contribute to `cost_eur`. Stored as kWh — if the user enters a
-- percentage instead, the application converts at write time.
ALTER TABLE charging_session
    ADD COLUMN solar_kwh NUMERIC(6, 3);
