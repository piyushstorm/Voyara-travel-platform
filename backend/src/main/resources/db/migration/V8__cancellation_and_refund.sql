-- ============================================================
-- V8: Comprehensive Cancellation, Refund & Policy Engine Enhancements
-- ============================================================

-- ── CANCELLATION POLICIES TABLE (Safe table creation) ──────────
CREATE TABLE IF NOT EXISTS cancellation_policies (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    min_hours_before INT NOT NULL,
    max_hours_before INT NOT NULL,
    refund_percentage NUMERIC(5, 2) NOT NULL,
    cancellation_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00
);

CREATE INDEX IF NOT EXISTS idx_cancellation_policies_lookup 
    ON cancellation_policies (entity_type, min_hours_before);

-- ── REFUNDS TABLE ENHANCEMENTS ──────────────────────────────
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'INR';
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS cancellation_comment VARCHAR(500);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS cancellation_policy VARCHAR(255);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(255);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_refund_idempotency ON refunds (idempotency_key);

-- ── SEED POLICIES FOR HOLIDAY, TRAIN, BUS, CAB ───────────────
-- Flight and Hotel are also seeded if absent
INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'FLIGHT', 'More than 7 days', 168, 99999, 100.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'FLIGHT' AND min_hours_before = 168);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'FLIGHT', '3-7 days', 72, 168, 70.00, 200.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'FLIGHT' AND min_hours_before = 72);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'FLIGHT', '24-72 hours', 24, 72, 50.00, 500.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'FLIGHT' AND min_hours_before = 24);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'FLIGHT', '12-24 hours', 12, 24, 25.00, 1000.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'FLIGHT' AND min_hours_before = 12);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'FLIGHT', 'Less than 12 hours', 0, 12, 0.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'FLIGHT' AND min_hours_before = 0);

-- Hotel
INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'HOTEL', 'More than 7 days', 168, 99999, 100.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'HOTEL' AND min_hours_before = 168);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'HOTEL', '3-7 days', 72, 168, 80.00, 500.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'HOTEL' AND min_hours_before = 72);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'HOTEL', '24-72 hours', 24, 72, 50.00, 1000.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'HOTEL' AND min_hours_before = 24);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'HOTEL', 'Less than 24 hours', 0, 24, 0.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'HOTEL' AND min_hours_before = 0);

-- Holiday Packages
INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'HOLIDAY', 'More than 7 days', 168, 99999, 100.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'HOLIDAY' AND min_hours_before = 168);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'HOLIDAY', '48-168 hours', 48, 168, 80.00, 1000.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'HOLIDAY' AND min_hours_before = 48);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'HOLIDAY', '24-48 hours', 24, 48, 50.00, 2000.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'HOLIDAY' AND min_hours_before = 24);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'HOLIDAY', 'Less than 24 hours', 0, 24, 0.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'HOLIDAY' AND min_hours_before = 0);

-- Train
INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'TRAIN', 'More than 48 hours', 48, 99999, 100.00, 100.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'TRAIN' AND min_hours_before = 48);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'TRAIN', '24-48 hours', 24, 48, 75.00, 200.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'TRAIN' AND min_hours_before = 24);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'TRAIN', '4-24 hours', 4, 24, 50.00, 300.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'TRAIN' AND min_hours_before = 4);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'TRAIN', 'Less than 4 hours', 0, 4, 0.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'TRAIN' AND min_hours_before = 0);

-- Bus
INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'BUS', 'More than 24 hours', 24, 99999, 90.00, 50.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'BUS' AND min_hours_before = 24);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'BUS', '12-24 hours', 12, 24, 50.00, 100.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'BUS' AND min_hours_before = 12);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'BUS', 'Less than 12 hours', 0, 12, 0.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'BUS' AND min_hours_before = 0);

-- Cab
INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'CAB', 'More than 4 hours', 4, 99999, 100.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'CAB' AND min_hours_before = 4);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'CAB', '1-4 hours', 1, 4, 70.00, 100.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'CAB' AND min_hours_before = 1);

INSERT INTO cancellation_policies (entity_type, name, min_hours_before, max_hours_before, refund_percentage, cancellation_fee)
SELECT 'CAB', 'Less than 1 hour', 0, 1, 0.00, 0.00
WHERE NOT EXISTS (SELECT 1 FROM cancellation_policies WHERE entity_type = 'CAB' AND min_hours_before = 0);
