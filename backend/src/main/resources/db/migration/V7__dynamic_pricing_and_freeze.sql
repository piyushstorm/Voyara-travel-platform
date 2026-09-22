-- ============================================================
-- V7: Dynamic Pricing, Price History & Price Freeze
-- ============================================================

-- ── PRICE HISTORY TABLE ──────────────────────────────────
CREATE TABLE IF NOT EXISTS price_history (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    cabin_class VARCHAR(50),
    previous_price DECIMAL(12, 2),
    new_price DECIMAL(12, 2) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    base_price DECIMAL(12, 2),
    demand_multiplier DECIMAL(5, 2),
    seasonal_multiplier DECIMAL(5, 2),
    inventory_multiplier DECIMAL(5, 2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_price_history_entity ON price_history(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_price_history_created ON price_history(created_at);

-- ── PRICE FREEZES TABLE ──────────────────────────────────
CREATE TABLE IF NOT EXISTS price_freezes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    cabin_class VARCHAR(50),
    frozen_price DECIMAL(12, 2) NOT NULL,
    freeze_fee DECIMAL(8, 2) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    booking_id BIGINT REFERENCES bookings(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_price_freeze_user_status ON price_freezes(user_id, status);
CREATE INDEX IF NOT EXISTS idx_price_freeze_entity_status ON price_freezes(entity_type, entity_id, status);
CREATE INDEX IF NOT EXISTS idx_price_freeze_expires ON price_freezes(expires_at);

-- ── PRICING RULES (ADMIN CONFIGURATION) ───────────────────
CREATE TABLE IF NOT EXISTS pricing_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    rule_type VARCHAR(50) NOT NULL,
    multiplier DECIMAL(5, 2) NOT NULL,
    threshold_value VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed default pricing rules
INSERT INTO pricing_rules (rule_name, rule_type, multiplier, threshold_value, is_active, description)
VALUES 
('PEAK_HOLIDAY_SURGE', 'SEASONAL', 1.20, 'HOLIDAY_PERIODS', true, 'Holiday and peak period markup (+20%)'),
('WEEKEND_SURGE', 'SEASONAL', 1.10, 'FRI_SUN', true, 'Weekend getaway surge (+10%)'),
('HIGH_DEMAND_SURGE', 'DEMAND', 1.15, 'BOOKED_RATIO_GT_75', true, 'High booking demand surge (+15%)'),
('LOW_INVENTORY_SURGE', 'INVENTORY', 1.10, 'REMAINING_LT_15_PCT', true, 'Scarcity / low inventory markup (+10%)'),
('MAX_MULTIPLIER_CAP', 'GUARDRAIL', 2.00, 'MAX_2X', true, 'Safety guardrail cap (2.0x base price)')
ON CONFLICT (rule_name) DO NOTHING;
