-- ============================================================
-- V2: Voyara Differentiator Features
-- Travel Guardian, Connection Risk, Trip Readiness,
-- Smart Timeline, Group Trips, Expense Split
-- ============================================================

-- ── TRAVEL GUARDIAN ALERTS ─────────────────────────────
CREATE TABLE IF NOT EXISTS guardian_alerts (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    action_label VARCHAR(100),
    action_route VARCHAR(200),
    related_flight_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    idempotency_key VARCHAR(200) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_guardian_alert_user ON guardian_alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_guardian_alert_booking ON guardian_alerts(booking_id);
CREATE INDEX IF NOT EXISTS idx_guardian_alert_unread ON guardian_alerts(user_id, is_read);

-- ── CONNECTION RISK SCORES ─────────────────────────────
CREATE TABLE IF NOT EXISTS connection_risks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    first_flight_id BIGINT NOT NULL REFERENCES flights(id),
    second_flight_id BIGINT NOT NULL REFERENCES flights(id),
    risk_level VARCHAR(20) NOT NULL,
    connection_minutes INTEGER NOT NULL,
    required_minutes INTEGER NOT NULL DEFAULT 90,
    risk_factors TEXT,
    last_calculated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_conn_risk_user ON connection_risks(user_id);
CREATE INDEX IF NOT EXISTS idx_conn_risk_booking ON connection_risks(booking_id);

-- ── TRIP READINESS ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS trip_readiness (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    score INTEGER NOT NULL DEFAULT 0,
    checks_json TEXT NOT NULL,
    last_calculated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(booking_id)
);
CREATE INDEX IF NOT EXISTS idx_readiness_user ON trip_readiness(user_id);

-- ── TRIP TIMELINE EVENTS ───────────────────────────────
CREATE TABLE IF NOT EXISTS trip_timeline_events (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,
    event_title VARCHAR(200) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    event_location VARCHAR(200),
    event_description TEXT,
    icon VARCHAR(20),
    is_affected BOOLEAN NOT NULL DEFAULT FALSE,
    affected_reason TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_timeline_booking ON trip_timeline_events(booking_id);
CREATE INDEX IF NOT EXISTS idx_timeline_user ON trip_timeline_events(user_id);
CREATE INDEX IF NOT EXISTS idx_timeline_time ON trip_timeline_events(booking_id, event_time);

-- ── GROUP TRIPS ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS group_trips (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    description TEXT,
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_group_trip_owner ON group_trips(owner_id);

-- ── GROUP TRIP BOOKINGS (link bookings to group trips) ──
CREATE TABLE IF NOT EXISTS group_trip_bookings (
    id BIGSERIAL PRIMARY KEY,
    group_trip_id BIGINT NOT NULL REFERENCES group_trips(id) ON DELETE CASCADE,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    added_by_user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(group_trip_id, booking_id)
);

-- ── TRAVEL COMPANIONS ──────────────────────────────────
CREATE TABLE IF NOT EXISTS travel_companions (
    id BIGSERIAL PRIMARY KEY,
    group_trip_id BIGINT NOT NULL REFERENCES group_trips(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL DEFAULT 'COMPANION',
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    joined_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(group_trip_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_companion_trip ON travel_companions(group_trip_id);
CREATE INDEX IF NOT EXISTS idx_companion_user ON travel_companions(user_id);

-- ── TRIP INVITATIONS ───────────────────────────────────
CREATE TABLE IF NOT EXISTS trip_invitations (
    id BIGSERIAL PRIMARY KEY,
    group_trip_id BIGINT NOT NULL REFERENCES group_trips(id) ON DELETE CASCADE,
    inviter_user_id BIGINT NOT NULL REFERENCES users(id),
    invitee_email VARCHAR(150) NOT NULL,
    invitee_user_id BIGINT REFERENCES users(id),
    token VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message TEXT,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    declined_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_invite_token ON trip_invitations(token);
CREATE INDEX IF NOT EXISTS idx_invite_email ON trip_invitations(invitee_email);
CREATE INDEX IF NOT EXISTS idx_invite_trip ON trip_invitations(group_trip_id);

-- ── TRIP EXPENSES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS trip_expenses (
    id BIGSERIAL PRIMARY KEY,
    group_trip_id BIGINT NOT NULL REFERENCES group_trips(id) ON DELETE CASCADE,
    paid_by_user_id BIGINT NOT NULL REFERENCES users(id),
    description VARCHAR(200) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    expense_type VARCHAR(30) NOT NULL,
    split_mode VARCHAR(20) NOT NULL DEFAULT 'EQUAL',
    booking_id BIGINT REFERENCES bookings(id),
    expense_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_expense_trip ON trip_expenses(group_trip_id);

-- ── EXPENSE PARTICIPANTS ───────────────────────────────
CREATE TABLE IF NOT EXISTS trip_expense_participants (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES trip_expenses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    share_amount NUMERIC(12,2) NOT NULL,
    share_percentage NUMERIC(5,2),
    is_settled BOOLEAN NOT NULL DEFAULT FALSE,
    settled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(expense_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_expense_part_user ON trip_expense_participants(user_id);

-- ── EXPENSE SETTLEMENTS ────────────────────────────────
CREATE TABLE IF NOT EXISTS trip_settlements (
    id BIGSERIAL PRIMARY KEY,
    group_trip_id BIGINT NOT NULL REFERENCES group_trips(id),
    from_user_id BIGINT NOT NULL REFERENCES users(id),
    to_user_id BIGINT NOT NULL REFERENCES users(id),
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settled_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_settlement_trip ON trip_settlements(group_trip_id);
CREATE INDEX IF NOT EXISTS idx_settlement_from ON trip_settlements(from_user_id);
CREATE INDEX IF NOT EXISTS idx_settlement_to ON trip_settlements(to_user_id);
