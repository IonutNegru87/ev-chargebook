-- Target state-of-charge reported alongside the energy state. Drives the
-- "time to N%" label and lets us reconstruct what the car was aiming for at
-- the time of each sample (the user can change it in the Volvo app).
ALTER TABLE charging_snapshot
    ADD COLUMN target_soc_pct INT;
