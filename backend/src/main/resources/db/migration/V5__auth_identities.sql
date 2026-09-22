-- V5: Authentication identities for multi-provider login
-- Supports LOCAL (email/password), GOOGLE, and PHONE (OTP) login methods

CREATE TABLE auth_identities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,          -- LOCAL, GOOGLE, PHONE
    provider_subject VARCHAR(255) NOT NULL,  -- Google sub ID, E.164 phone, or email
    provider_email VARCHAR(255),             -- Google email (for discovery)
    display_name VARCHAR(255),               -- Google name, etc.
    phone_number VARCHAR(20),                -- E.164 format for PHONE provider
    verified BOOLEAN NOT NULL DEFAULT FALSE, -- Whether the identity is verified
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_provider_subject UNIQUE (provider, provider_subject)
);

-- Index for fast lookup by provider + subject (login flow)
CREATE INDEX idx_auth_identity_provider_subject ON auth_identities (provider, provider_subject);

-- Index for looking up all identities for a user (profile/settings)
CREATE INDEX idx_auth_identity_user_id ON auth_identities (user_id);

-- Ensure phone uniqueness when normalized to E.164
CREATE UNIQUE INDEX idx_auth_identity_phone ON auth_identities (phone_number)
    WHERE phone_number IS NOT NULL;

-- Add phone column to users table if not exists
DO $$ BEGIN
    ALTER TABLE users ADD COLUMN phone VARCHAR(20);
EXCEPTION WHEN duplicate_column THEN
    -- Column already exists
END $$;
