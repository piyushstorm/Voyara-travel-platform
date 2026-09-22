-- Add email_verified boolean to users table, defaulting to true for existing users to avoid lockout
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT true;

-- Create email verification tokens table
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_token ON email_verification_tokens(token_hash);
CREATE INDEX idx_email_verification_user ON email_verification_tokens(user_id);
