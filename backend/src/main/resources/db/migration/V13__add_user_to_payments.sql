ALTER TABLE payments ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id);
CREATE INDEX IF NOT EXISTS idx_payment_user ON payments (user_id);
