-- ============================================================
-- V6: Flight Status Enhancements
-- Columns for realistic tracking, dynamic ETA, and simulation
-- ============================================================

ALTER TABLE flight_statuses ADD COLUMN IF NOT EXISTS delay_minutes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE flight_statuses ADD COLUMN IF NOT EXISTS actual_departure TIMESTAMP;
ALTER TABLE flight_statuses ADD COLUMN IF NOT EXISTS actual_arrival TIMESTAMP;
ALTER TABLE flight_statuses ADD COLUMN IF NOT EXISTS boarding_time TIMESTAMP;
ALTER TABLE flight_statuses ADD COLUMN IF NOT EXISTS scenario VARCHAR(50) DEFAULT 'ON_TIME';
ALTER TABLE flight_statuses ADD COLUMN IF NOT EXISTS last_simulated_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_flight_status_status ON flight_statuses(status);
CREATE INDEX IF NOT EXISTS idx_flight_status_updated ON flight_statuses(updated_at);
