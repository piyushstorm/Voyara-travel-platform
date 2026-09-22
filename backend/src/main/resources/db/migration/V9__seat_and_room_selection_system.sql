-- ============================================================
-- V9: Interactive Seat and Room Selection System
-- Flight Seat Enhancements, Hotel Room Inventory Holds,
-- User Travel Preferences, and Concurrency Controls
-- ============================================================

-- ── 1. FLIGHT SEAT SCHEMA ENHANCEMENTS ──────────────────────
ALTER TABLE seats ADD COLUMN IF NOT EXISTS seat_type VARCHAR(30) DEFAULT 'STANDARD';
ALTER TABLE seats ADD COLUMN IF NOT EXISTS is_middle BOOLEAN DEFAULT FALSE;
ALTER TABLE seats ADD COLUMN IF NOT EXISTS is_extra_legroom BOOLEAN DEFAULT FALSE;
ALTER TABLE seats ADD COLUMN IF NOT EXISTS is_emergency_exit BOOLEAN DEFAULT FALSE;
ALTER TABLE seats ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'INR';

-- Populate middle seat flags and default seat types based on column and row
UPDATE seats SET is_middle = TRUE WHERE column_letter IN ('B', 'E') AND is_middle = FALSE;
UPDATE seats SET is_extra_legroom = TRUE, seat_type = 'EXTRA_LEGROOM' WHERE row_number IN (1, 12, 13) AND is_extra_legroom = FALSE;
UPDATE seats SET is_emergency_exit = TRUE, seat_type = 'EXIT_ROW' WHERE row_number IN (12, 13) AND is_emergency_exit = FALSE;
UPDATE seats SET seat_type = 'PREMIUM' WHERE (premium_surcharge IS NOT NULL AND premium_surcharge > 0) AND seat_type = 'STANDARD';

CREATE INDEX IF NOT EXISTS idx_seat_type ON seats (seat_type);
CREATE INDEX IF NOT EXISTS idx_seat_available ON seats (flight_id, available);

-- ── 2. HOTEL ROOM SCHEMA ENHANCEMENTS ───────────────────────
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS amenities TEXT;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS images_json TEXT;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'INR';

-- Populate standard amenities for existing rooms based on room_type
UPDATE rooms SET amenities = 'Free Wi-Fi,Air Conditioning,Ensuite Bathroom,LED TV,Work Desk,Coffee/Tea Maker'
WHERE amenities IS NULL AND room_type = 'STANDARD';

UPDATE rooms SET amenities = 'Free Wi-Fi,Air Conditioning,City View,Mini Bar,King Bed,Smart TV,Bathrobes,Free Breakfast,Coffee Machine'
WHERE amenities IS NULL AND room_type = 'DELUXE';

UPDATE rooms SET amenities = 'Panoramic View,Executive Lounge Access,Living Area,Espresso Machine,Luxury Toiletries,Jacuzzi,Breakfast Included,Priority Check-in'
WHERE amenities IS NULL AND room_type IN ('SUITE', 'PRESIDENTIAL');

-- ── 3. HOTEL ROOM INVENTORY HOLDS ───────────────────────────
CREATE TABLE IF NOT EXISTS room_holds (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_room_hold_room ON room_holds (room_id);
CREATE INDEX IF NOT EXISTS idx_room_hold_user ON room_holds (user_id);
CREATE INDEX IF NOT EXISTS idx_room_hold_status ON room_holds (status);
CREATE INDEX IF NOT EXISTS idx_room_hold_expires ON room_holds (expires_at);

-- ── 4. USER TRAVEL PREFERENCES ──────────────────────────────
CREATE TABLE IF NOT EXISTS user_travel_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    preferred_seat_position VARCHAR(30) DEFAULT 'WINDOW',
    preferred_seat_type VARCHAR(30) DEFAULT 'STANDARD',
    preferred_room_type VARCHAR(30) DEFAULT 'DELUXE',
    preferred_bed_type VARCHAR(30) DEFAULT 'KING',
    preferred_room_features VARCHAR(255) DEFAULT 'CITY_VIEW,HIGH_FLOOR',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_travel_pref_user ON user_travel_preferences (user_id);
