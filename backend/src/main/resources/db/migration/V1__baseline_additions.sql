-- ============================================================
-- V1: Safe baseline additions for existing schema
-- ============================================================
-- This migration adds only safe incremental improvements.
-- It assumes all tables already exist (created by Hibernate ddl-auto=update).
-- No tables are created or dropped.
-- ============================================================

-- ── USERS ──────────────────────────────────────────────
-- Email unique constraint (already defined in JPA, ensure at DB level)
-- Note: skipped IF EXISTS check because H2/PG handle this differently.
-- Safe to run on existing DB with unique email constraint already in place.

-- ── BOOKINGS ───────────────────────────────────────────
-- Booking reference unique index (already in JPA, verify)
CREATE INDEX IF NOT EXISTS idx_booking_created ON bookings (created_at);
CREATE INDEX IF NOT EXISTS idx_booking_reference ON bookings (booking_reference);

-- ── PAYMENTS ───────────────────────────────────────────
-- Critical for payment lookups by booking, Razorpay IDs, status
CREATE INDEX IF NOT EXISTS idx_payment_booking ON payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payment_created ON payments (created_at);
CREATE INDEX IF NOT EXISTS idx_payment_razorpay_order ON payments (razorpay_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_razorpay_payment ON payments (razorpay_payment_id);

-- ── REFUNDS ────────────────────────────────────────────
-- Refund lookups by booking, status, Razorpay refund ID
CREATE INDEX IF NOT EXISTS idx_refund_booking ON refunds (booking_id);
CREATE INDEX IF NOT EXISTS idx_refund_status ON refunds (status);
CREATE INDEX IF NOT EXISTS idx_refund_created ON refunds (created_at);
CREATE INDEX IF NOT EXISTS idx_refund_razorpay ON refunds (razorpay_refund_id);

-- ── REFRESH TOKENS ─────────────────────────────────────
-- Token lookup and expiry cleanup
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON refresh_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_token_revoked ON refresh_tokens (revoked);

-- ── NOTIFICATIONS ──────────────────────────────────────
-- Delivery status for async email processing
CREATE INDEX IF NOT EXISTS idx_notif_delivery ON notifications (delivery_status);

-- ── AUDIT LOGS ─────────────────────────────────────────
-- Audit log queries by actor, action, date
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_logs (actor_email);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_target ON audit_logs (target_user_id);

-- ── SAVED TRAVELLERS ───────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_saved_traveller_user ON saved_travellers (user_id);

-- ── REWARDS ────────────────────────────────────────────
-- Reward account already has unique constraint on user_id via JPA
-- Reward transactions already have indexes defined in JPA

-- ── COUPONS ────────────────────────────────────────────
-- Coupon code lookup (already unique via JPA)
CREATE INDEX IF NOT EXISTS idx_coupon_active ON coupons (active);
CREATE INDEX IF NOT EXISTS idx_coupon_expiry ON coupons (expiry_date);

-- ── WEBHOOK EVENTS ─────────────────────────────────────
-- Event ID already has unique index via JPA
CREATE INDEX IF NOT EXISTS idx_webhook_created ON webhook_events (created_at);

-- ── SEATS ──────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_seat_flight ON seats (flight_id);
CREATE INDEX IF NOT EXISTS idx_seat_cabin ON seats (cabin_class);

-- ── FARE OPTIONS ───────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_fare_flight ON fare_options (flight_id);

-- ── ROOMS ──────────────────────────────────────────────
-- hotel_id index already exists via JPA

-- ── REVIEWS ────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_review_hotel ON reviews (hotel_id);
CREATE INDEX IF NOT EXISTS idx_review_user ON reviews (user_id);
CREATE INDEX IF NOT EXISTS idx_review_status ON reviews (status);

-- ── FLIGHT STATUS ──────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_flight_status_flight ON flight_statuses (flight_id);

-- ── SEAT HOLDS ─────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_seat_hold_flight ON seat_holds (flight_id);
CREATE INDEX IF NOT EXISTS idx_seat_hold_user ON seat_holds (user_id);
CREATE INDEX IF NOT EXISTS idx_seat_hold_expires ON seat_holds (expires_at);
