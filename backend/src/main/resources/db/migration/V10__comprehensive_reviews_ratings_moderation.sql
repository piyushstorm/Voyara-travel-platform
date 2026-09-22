-- =====================================================
-- V10: Comprehensive Review, Rating, Photo, Reply & Moderation System
-- =====================================================

-- 1. Enhance reviews table
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS booking_id BIGINT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS report_count INT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_review_booking' AND table_name = 'reviews'
    ) THEN
        ALTER TABLE reviews ADD CONSTRAINT fk_review_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Enforce one review per booking for a user (where booking_id is not null)
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_user_booking_review
ON reviews (user_id, booking_id)
WHERE booking_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_review_report_count ON reviews (report_count);
CREATE INDEX IF NOT EXISTS idx_review_flight_status ON reviews (flight_id, status);
CREATE INDEX IF NOT EXISTS idx_review_hotel_status ON reviews (hotel_id, status);

-- 2. Create review_reports table
CREATE TABLE IF NOT EXISTS review_reports (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    reporter_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_user_review_report UNIQUE (reporter_user_id, review_id)
);

CREATE INDEX IF NOT EXISTS idx_report_review ON review_reports (review_id);
CREATE INDEX IF NOT EXISTS idx_report_status ON review_reports (status);

-- 3. Create review_moderation_actions table
CREATE TABLE IF NOT EXISTS review_moderation_actions (
    id BIGSERIAL PRIMARY KEY,
    moderator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mod_action_review ON review_moderation_actions (review_id);
CREATE INDEX IF NOT EXISTS idx_mod_action_moderator ON review_moderation_actions (moderator_id);
CREATE INDEX IF NOT EXISTS idx_mod_action_created ON review_moderation_actions (created_at);

-- 4. Enhance review_photos table
ALTER TABLE review_photos ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE review_photos ADD COLUMN IF NOT EXISTS content_type VARCHAR(100);
