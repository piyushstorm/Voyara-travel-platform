-- ============================================================
-- V14: Add missing sent_at column to trip_invitations
-- Fixes Hibernate schema-validation failure on trip_invitations
-- ============================================================

ALTER TABLE trip_invitations ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP;
