-- ============================================================
-- V12: Fix connection_risks.booking_id to be nullable
-- ConnectionRiskService calculates risk between flight pairs
-- without necessarily having a single booking context.
-- ============================================================

ALTER TABLE connection_risks ALTER COLUMN booking_id DROP NOT NULL;
