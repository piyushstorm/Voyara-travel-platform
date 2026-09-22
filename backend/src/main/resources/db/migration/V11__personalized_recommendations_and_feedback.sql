-- ============================================================
-- V11: Personalized Recommendation Engine, Destination Taxonomy & Feedback Loop
-- ============================================================

-- ── 1. DESTINATIONS TAXONOMY ─────────────────────────────────
CREATE TABLE IF NOT EXISTS destinations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL, -- BEACH, MOUNTAIN, HERITAGE, CITY, LUXURY, ADVENTURE
    tags TEXT NOT NULL,           -- Comma-separated: BEACH,NIGHTLIFE,RELAXATION
    climate VARCHAR(50),
    best_season VARCHAR(100),
    average_daily_budget NUMERIC(10, 2),
    popularity_score DOUBLE PRECISION DEFAULT 0.8,
    image_url TEXT,
    description TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_destination_category ON destinations(category);
CREATE INDEX IF NOT EXISTS idx_destination_popularity ON destinations(popularity_score);

-- ── 2. RECOMMENDATIONS TABLE ──────────────────────────────────
CREATE TABLE IF NOT EXISTS recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type VARCHAR(30) NOT NULL, -- HOTEL, FLIGHT, DESTINATION, HOLIDAY_PACKAGE
    entity_id BIGINT NOT NULL,
    score NUMERIC(5, 2) NOT NULL,
    algorithm VARCHAR(50) NOT NULL,   -- HYBRID, CONTENT_BASED, COLLABORATIVE, POPULAR_COLD_START
    reason VARCHAR(500) NOT NULL,
    structured_reasons_json TEXT,
    feedback VARCHAR(30),             -- HELPFUL, IRRELEVANT
    batch_cycle VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Add structured_reasons_json if recommendations table was created prior
ALTER TABLE recommendations ADD COLUMN IF NOT EXISTS structured_reasons_json TEXT;
ALTER TABLE recommendations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_rec_user ON recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_rec_type ON recommendations(entity_type);
CREATE INDEX IF NOT EXISTS idx_rec_user_type ON recommendations(user_id, entity_type);

-- ── 3. RECOMMENDATION FEEDBACK (PERMANENT FEEDBACK STORE) ────
CREATE TABLE IF NOT EXISTS recommendation_feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recommendation_id BIGINT REFERENCES recommendations(id) ON DELETE SET NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    feedback_type VARCHAR(30) NOT NULL, -- HELPFUL, IRRELEVANT
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_entity_feedback UNIQUE (user_id, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_rec_feedback_user ON recommendation_feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_rec_feedback_type ON recommendation_feedback(feedback_type);

-- ── 4. EXTEND USER TRAVEL PREFERENCES ─────────────────────────
ALTER TABLE user_travel_preferences ADD COLUMN IF NOT EXISTS preferred_destinations VARCHAR(255);
ALTER TABLE user_travel_preferences ADD COLUMN IF NOT EXISTS preferred_cabin_class VARCHAR(50);
ALTER TABLE user_travel_preferences ADD COLUMN IF NOT EXISTS budget_level VARCHAR(50);
