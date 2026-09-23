-- ============================================================
-- V1: Initial Base Schema & Core Indexes
-- ============================================================
-- Creates baseline application tables, primary keys, unique constraints,
-- foreign keys, and indexes for Voyara Travel Platform.
-- Safe for fresh PostgreSQL databases and pre-existing databases.
-- ============================================================

-- ── USERS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    email character varying(150) NOT NULL,
    enabled boolean NOT NULL,
    name character varying(100) NOT NULL,
    password character varying(128) NOT NULL,
    password_reset_token character varying(255),
    password_reset_token_expiry timestamp(6) without time zone,
    role character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[])))
);

-- ── AIRPORTS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS airports (
    id BIGSERIAL PRIMARY KEY,
    city character varying(255) NOT NULL,
    code character varying(3) NOT NULL,
    country character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    name character varying(255) NOT NULL
);

-- ── AIRLINES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS airlines (
    id BIGSERIAL PRIMARY KEY,
    code character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    logo_url character varying(255),
    name character varying(255) NOT NULL,
    rating double precision NOT NULL
);

-- ── FLIGHTS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS flights (
    id BIGSERIAL PRIMARY KEY,
    active boolean NOT NULL,
    arrival_time timestamp(6) without time zone NOT NULL,
    booked_seats_business integer NOT NULL,
    booked_seats_economy integer NOT NULL,
    booked_seats_first integer NOT NULL,
    booked_seats_premium_economy integer NOT NULL,
    business_base_price numeric(10,2),
    business_price numeric(10,2) NOT NULL,
    created_at timestamp(6) without time zone,
    departure_date date NOT NULL,
    departure_time timestamp(6) without time zone NOT NULL,
    destination_code character varying(3) NOT NULL,
    duration_minutes integer NOT NULL,
    economy_base_price numeric(10,2),
    economy_price numeric(10,2) NOT NULL,
    first_class_base_price numeric(10,2),
    first_class_price numeric(10,2) NOT NULL,
    flight_number character varying(255) NOT NULL,
    origin_code character varying(3) NOT NULL,
    premium_economy_base_price numeric(10,2),
    premium_economy_price numeric(10,2) NOT NULL,
    stopover_city character varying(255),
    stops integer NOT NULL,
    total_seats_business integer NOT NULL,
    total_seats_economy integer NOT NULL,
    total_seats_first integer NOT NULL,
    total_seats_premium_economy integer NOT NULL,
    updated_at timestamp(6) without time zone,
    airline_id bigint NOT NULL,
    destination_id bigint NOT NULL,
    origin_id bigint NOT NULL
);

-- ── HOTELS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS hotels (
    id BIGSERIAL PRIMARY KEY,
    active boolean NOT NULL,
    address character varying(255),
    city character varying(255) NOT NULL,
    country character varying(255),
    created_at timestamp(6) without time zone,
    description character varying(255),
    guest_rating double precision NOT NULL,
    image_url character varying(255),
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    name character varying(255) NOT NULL,
    review_count integer NOT NULL,
    star_rating integer NOT NULL,
    starting_price numeric(38,2),
    updated_at timestamp(6) without time zone
);

-- ── HOTEL_AMENITIES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS hotel_amenities (
    hotel_id bigint NOT NULL,
    amenity character varying(255)
);

-- ── HOTEL_IMAGES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS hotel_images (
    hotel_id bigint NOT NULL,
    image_url character varying(255)
);

-- ── ROOMS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS rooms (
    id BIGSERIAL PRIMARY KEY,
    active boolean NOT NULL,
    available_rooms integer NOT NULL,
    base_price numeric(10,2),
    bed_count integer NOT NULL,
    bed_type character varying(255),
    created_at timestamp(6) without time zone,
    description character varying(255),
    image_url character varying(255),
    max_guests integer NOT NULL,
    name character varying(255) NOT NULL,
    price_per_night numeric(10,2) NOT NULL,
    room_type character varying(255) NOT NULL,
    size_sqm double precision NOT NULL,
    total_rooms integer NOT NULL,
    updated_at timestamp(6) without time zone,
    hotel_id bigint NOT NULL
);

-- ── SEATS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    is_aisle boolean,
    available boolean NOT NULL,
    cabin_class character varying(255) NOT NULL,
    column_letter character varying(255),
    created_at timestamp(6) without time zone,
    held_by_user_id character varying(255),
    held_until timestamp(6) without time zone,
    premium_surcharge numeric(8,2),
    price numeric(38,2) NOT NULL,
    row_number integer NOT NULL,
    seat_number character varying(255) NOT NULL,
    version bigint,
    is_window boolean,
    flight_id bigint NOT NULL
);

-- ── FARE_OPTIONS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS fare_options (
    id BIGSERIAL PRIMARY KEY,
    cabin_baggage_dimensions character varying(255),
    cabin_baggage_kg integer NOT NULL,
    change_fee character varying(255),
    changeable boolean NOT NULL,
    checked_baggage_kg integer NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(255),
    fare_type character varying(255) NOT NULL,
    lounge_access boolean NOT NULL,
    meal_included boolean NOT NULL,
    name character varying(255) NOT NULL,
    override_price numeric(10,2),
    price_multiplier numeric(4,2) NOT NULL,
    priority_boarding boolean NOT NULL,
    refund_policy character varying(255),
    refundable boolean NOT NULL,
    seat_selection_included boolean NOT NULL,
    flight_id bigint NOT NULL
);

-- ── BUSES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS buses (
    id BIGSERIAL PRIMARY KEY,
    active boolean,
    amenities text,
    arrival_time time(6) without time zone NOT NULL,
    available_seats integer,
    base_price numeric(38,2),
    boarding_points text,
    bus_number character varying(255) NOT NULL,
    bus_type character varying(255),
    departure_time time(6) without time zone NOT NULL,
    destination_city character varying(255),
    destination_code character varying(255),
    destination_name character varying(255),
    dropping_points text,
    duration_minutes integer,
    operator_name character varying(255) NOT NULL,
    origin_city character varying(255),
    origin_code character varying(255),
    origin_name character varying(255),
    rating double precision,
    running_days character varying(255),
    total_seats integer
);

-- ── CABS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS cabs (
    id BIGSERIAL PRIMARY KEY,
    ac boolean,
    active boolean,
    base_fare numeric(38,2),
    booking_fee numeric(38,2),
    capacity integer,
    features text,
    luggage_capacity integer,
    minimum_fare numeric(38,2),
    per_km_rate numeric(38,2),
    rating double precision,
    vehicle_name character varying(255) NOT NULL,
    vehicle_type character varying(255) NOT NULL
);

-- ── TRAINS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS trains (
    id BIGSERIAL PRIMARY KEY,
    active boolean,
    arrival_time time(6) without time zone NOT NULL,
    classes text,
    departure_time time(6) without time zone NOT NULL,
    destination_city character varying(255),
    destination_code character varying(255),
    destination_name character varying(255),
    duration_minutes integer,
    intermediate_stations text,
    origin_city character varying(255),
    origin_code character varying(255),
    origin_name character varying(255),
    running_days character varying(255),
    train_name character varying(255) NOT NULL,
    train_number character varying(255) NOT NULL,
    train_type character varying(255)
);

-- ── TRAIN_SEATS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS train_seats (
    id BIGSERIAL PRIMARY KEY,
    available_seats integer,
    class_code character varying(255) NOT NULL,
    class_name character varying(255) NOT NULL,
    fare numeric(38,2) NOT NULL,
    rac_seats integer,
    status character varying(255),
    total_seats integer,
    waitlist_count integer,
    train_id bigint NOT NULL
);

-- ── HOLIDAY_PACKAGES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS holiday_packages (
    id BIGSERIAL PRIMARY KEY,
    active boolean,
    activities text,
    currency character varying(255) NOT NULL,
    departure_city character varying(255),
    description text,
    destination character varying(255) NOT NULL,
    destination_code character varying(255),
    duration_days integer,
    duration_nights integer,
    exclusions text,
    featured boolean,
    highlights text,
    hotel_category character varying(255),
    image_url character varying(255),
    inclusions text,
    itinerary text,
    max_group_size integer,
    meal_plan character varying(255),
    original_price numeric(38,2),
    price_per_person numeric(38,2) NOT NULL,
    rating double precision,
    review_count integer,
    title character varying(255) NOT NULL,
    transport_type character varying(255),
    trip_type character varying(255) NOT NULL
);

-- ── ADD_ONS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS add_ons (
    id BIGSERIAL PRIMARY KEY,
    active boolean NOT NULL,
    applicability character varying(255),
    cabin_class character varying(255),
    category character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(255),
    name character varying(255) NOT NULL,
    price numeric(8,2) NOT NULL
);

-- ── BOOKINGS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_reference character varying(255) NOT NULL,
    booking_type character varying(255) NOT NULL,
    cabin_class character varying(255),
    cancellation_reason character varying(255),
    check_in_date timestamp(6) without time zone,
    check_out_date timestamp(6) without time zone,
    created_at timestamp(6) without time zone,
    number_of_nights integer NOT NULL,
    passenger_count integer NOT NULL,
    payment_id character varying(255),
    payment_method character varying(255),
    refund_amount numeric(12,2),
    selected_seat_numbers character varying(255),
    special_requests character varying(255),
    status character varying(255) NOT NULL,
    total_amount numeric(12,2) NOT NULL,
    travel_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    flight_id bigint,
    hotel_id bigint,
    room_id bigint,
    user_id bigint NOT NULL,
    coupon_code character varying(255),
    discount_amount numeric(38,2),
    original_amount numeric(38,2)
);

-- ── COUPON_USAGES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupon_usages (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    discount_amount numeric(10,2) NOT NULL,
    status character varying(20) NOT NULL,
    booking_id bigint,
    coupon_id bigint NOT NULL,
    user_id bigint NOT NULL
);

-- ── COUPONS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupons (
    id BIGSERIAL PRIMARY KEY,
    active boolean NOT NULL,
    applicable_module character varying(20) NOT NULL,
    code character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(500),
    discount_type character varying(255) NOT NULL,
    discount_value numeric(10,2) NOT NULL,
    expiry_date timestamp(6) without time zone NOT NULL,
    max_discount numeric(10,2),
    min_booking_amount numeric(10,2),
    per_user_limit integer NOT NULL,
    start_date timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    usage_limit integer NOT NULL,
    used_count integer NOT NULL
);

-- ── PAYMENTS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    amount numeric(12,2) NOT NULL,
    card_last4 character varying(255),
    created_at timestamp(6) without time zone,
    currency character varying(255) NOT NULL,
    failure_reason character varying(255),
    payment_id character varying(255) NOT NULL,
    payment_method character varying(255),
    status character varying(255) NOT NULL,
    booking_id bigint,
    captured_at timestamp(6) without time zone,
    razorpay_order_id character varying(255),
    razorpay_payment_id character varying(255),
    updated_at timestamp(6) without time zone
);

-- ── REFUNDS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS refunds (
    id BIGSERIAL PRIMARY KEY,
    cancellation_reason character varying(255),
    completed_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone,
    expected_completion_at timestamp(6) without time zone,
    external_refund_id character varying(255),
    original_amount numeric(12,2) NOT NULL,
    refund_amount numeric(12,2) NOT NULL,
    refund_id character varying(255) NOT NULL,
    refund_percentage numeric(5,2) NOT NULL,
    rejection_reason character varying(255),
    status character varying(255) NOT NULL,
    booking_id bigint NOT NULL,
    razorpay_refund_id character varying(255)
);

-- ── REFRESH_TOKENS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone NOT NULL,
    revoked boolean NOT NULL,
    token character varying(1024) NOT NULL,
    user_id bigint NOT NULL
);

-- ── NOTIFICATIONS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    channel character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    delivery_status character varying(255) NOT NULL,
    email_attempts integer NOT NULL,
    email_failure_reason character varying(255),
    idempotency_key character varying(255),
    is_read boolean NOT NULL,
    message text NOT NULL,
    read_at timestamp(6) without time zone,
    related_entity_id bigint,
    related_entity_ref character varying(255),
    related_entity_type character varying(255),
    title character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    user_id bigint NOT NULL
);

-- ── NOTIFICATION_PREFERENCES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    always_send_security_notifications boolean NOT NULL,
    created_at timestamp(6) without time zone,
    email_booking_updates boolean NOT NULL,
    email_cancellation_refund boolean NOT NULL,
    email_flight_updates boolean NOT NULL,
    email_marketing boolean NOT NULL,
    email_payment_updates boolean NOT NULL,
    email_price_alerts boolean NOT NULL,
    in_app_booking_updates boolean NOT NULL,
    in_app_flight_updates boolean NOT NULL,
    in_app_payment_updates boolean NOT NULL,
    in_app_price_alerts boolean NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id bigint NOT NULL
);

-- ── AUDIT_LOGS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action character varying(255) NOT NULL,
    actor_email character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    details character varying(255),
    new_role character varying(255) NOT NULL,
    old_role character varying(255) NOT NULL,
    target_user_email character varying(255) NOT NULL,
    target_user_id bigint NOT NULL
);

-- ── SAVED_TRAVELLERS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS saved_travellers (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    date_of_birth date,
    first_name character varying(255) NOT NULL,
    gender character varying(255),
    last_name character varying(255) NOT NULL,
    middle_name character varying(255),
    nationality character varying(255),
    passport_country character varying(255),
    passport_expiry date,
    passport_number character varying(255),
    title character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id bigint NOT NULL
);

-- ── SAVED_PREFERENCES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS saved_preferences (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    destination_code character varying(255),
    entity_id bigint,
    label character varying(255),
    origin_code character varying(255),
    type character varying(255) NOT NULL,
    user_id bigint NOT NULL
);

-- ── REWARD_ACCOUNTS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS reward_accounts (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    lifetime_points_earned bigint NOT NULL,
    lifetime_points_expired bigint NOT NULL,
    lifetime_points_redeemed bigint NOT NULL,
    points_balance bigint NOT NULL,
    qualifying_points bigint NOT NULL,
    tier character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id bigint NOT NULL
);

-- ── REWARD_CONFIG ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS reward_config (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    gold_earning_multiplier double precision NOT NULL,
    gold_tier_threshold bigint NOT NULL,
    max_redemption_percent integer NOT NULL,
    min_redemption_points integer NOT NULL,
    platinum_earning_multiplier double precision NOT NULL,
    platinum_tier_threshold bigint NOT NULL,
    points_expiry_days integer NOT NULL,
    points_per_hundred_rupees integer NOT NULL,
    points_per_rupee_redemption integer NOT NULL,
    updated_at timestamp(6) without time zone
);

-- ── REWARD_TRANSACTIONS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS reward_transactions (
    id BIGSERIAL PRIMARY KEY,
    balance_after bigint NOT NULL,
    booking_reference character varying(255),
    created_at timestamp(6) without time zone,
    description character varying(255),
    eligible_amount numeric(12,2),
    idempotency_key character varying(255),
    performed_by character varying(255),
    points bigint NOT NULL,
    transaction_type character varying(255) NOT NULL,
    user_id bigint NOT NULL
);

-- ── WEBHOOK_EVENTS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    event_id character varying(255) NOT NULL,
    event_type character varying(255) NOT NULL,
    payload_summary character varying(500),
    status character varying(255) NOT NULL
);

-- ── REVIEWS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    flag_reason character varying(255),
    helpful_count integer NOT NULL,
    not_helpful_count integer NOT NULL,
    rating integer NOT NULL,
    status character varying(255) NOT NULL,
    text character varying(2000),
    updated_at timestamp(6) without time zone,
    verified_booking boolean NOT NULL,
    flight_id bigint,
    hotel_id bigint,
    user_id bigint NOT NULL
);

-- ── REVIEW_PHOTOS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_photos (
    id BIGSERIAL PRIMARY KEY,
    caption character varying(255),
    created_at timestamp(6) without time zone,
    photo_url character varying(255) NOT NULL,
    review_id bigint NOT NULL
);

-- ── REVIEW_REPLIES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_replies (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    text character varying(1000) NOT NULL,
    review_id bigint NOT NULL,
    user_id bigint NOT NULL,
    updated_at timestamp(6) without time zone
);

-- ── REVIEW_VOTES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_votes (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    vote_type character varying(255) NOT NULL,
    review_id bigint NOT NULL,
    user_id bigint NOT NULL
);

-- ── FLIGHT_STATUSES ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS flight_statuses (
    id BIGSERIAL PRIMARY KEY,
    delay_reason character varying(255),
    estimated_arrival timestamp(6) without time zone,
    estimated_departure timestamp(6) without time zone,
    gate character varying(255),
    scheduled_arrival timestamp(6) without time zone,
    scheduled_departure timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    terminal character varying(255),
    updated_at timestamp(6) without time zone,
    flight_id bigint NOT NULL
);

-- ── FLIGHT_STATUS_HISTORY ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS flight_status_history (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    delay_reason character varying(255),
    message character varying(255),
    new_estimated_departure timestamp(6) without time zone,
    new_status character varying(255) NOT NULL,
    previous_estimated_departure timestamp(6) without time zone,
    previous_status character varying(255) NOT NULL,
    flight_id bigint NOT NULL
);

-- ── SEAT_HOLDS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS seat_holds (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone NOT NULL,
    status character varying(255) NOT NULL,
    seat_id bigint NOT NULL,
    user_id bigint NOT NULL
);

-- ── USER_INTERACTIONS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_interactions (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    entity_id bigint NOT NULL,
    entity_type character varying(255) NOT NULL,
    interaction_type character varying(255) NOT NULL,
    rating integer,
    tags character varying(500),
    user_id bigint NOT NULL
);


-- ============================================================
-- BASE UNIQUE CONSTRAINTS
-- ============================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'idx_notif_idempotency' AND table_schema = 'public') THEN
        ALTER TABLE notifications ADD CONSTRAINT idx_notif_idempotency UNIQUE (idempotency_key);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'idx_rt_unique_earning' AND table_schema = 'public') THEN
        ALTER TABLE reward_transactions ADD CONSTRAINT idx_rt_unique_earning UNIQUE (transaction_type, booking_reference);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'idx_webhook_event_id' AND table_schema = 'public') THEN
        ALTER TABLE webhook_events ADD CONSTRAINT idx_webhook_event_id UNIQUE (event_id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uk4nqpx8aip54l559nx2j7sx5a6' AND table_schema = 'public') THEN
        ALTER TABLE reward_accounts ADD CONSTRAINT uk4nqpx8aip54l559nx2j7sx5a6 UNIQUE (user_id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uk6bx3i9v6ikjiy0ru5ybor8t7' AND table_schema = 'public') THEN
        ALTER TABLE flights ADD CONSTRAINT uk6bx3i9v6ikjiy0ru5ybor8t7 UNIQUE (flight_number);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uk6dotkott2kjsp8vw4d0m25fb7' AND table_schema = 'public') THEN
        ALTER TABLE users ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uk6mqk0hlfb8oy9falq2r03dsat' AND table_schema = 'public') THEN
        ALTER TABLE airlines ADD CONSTRAINT uk6mqk0hlfb8oy9falq2r03dsat UNIQUE (code);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uk8x5wlokxte7yksdsllxtxbjf0' AND table_schema = 'public') THEN
        ALTER TABLE airports ADD CONSTRAINT uk8x5wlokxte7yksdsllxtxbjf0 UNIQUE (code);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uk9985dy9h3laa0hruo2ip09oeb' AND table_schema = 'public') THEN
        ALTER TABLE refunds ADD CONSTRAINT uk9985dy9h3laa0hruo2ip09oeb UNIQUE (refund_id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'uke92mgyq35mdeo8gc1un2o6uk0' AND table_schema = 'public') THEN
        ALTER TABLE bookings ADD CONSTRAINT uke92mgyq35mdeo8gc1un2o6uk0 UNIQUE (booking_reference);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'ukee09n28f6xc33qtcs92r1x057' AND table_schema = 'public') THEN
        ALTER TABLE review_votes ADD CONSTRAINT ukee09n28f6xc33qtcs92r1x057 UNIQUE (review_id, user_id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'ukeplt0kkm9yf2of2lnx6c1oy9b' AND table_schema = 'public') THEN
        ALTER TABLE coupons ADD CONSTRAINT ukeplt0kkm9yf2of2lnx6c1oy9b UNIQUE (code);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'ukgfq6ovqoch2jbsxo8g6l6l4ap' AND table_schema = 'public') THEN
        ALTER TABLE reward_transactions ADD CONSTRAINT ukgfq6ovqoch2jbsxo8g6l6l4ap UNIQUE (idempotency_key);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'ukghpmfn23vmxfu3spu3lfg4r2d' AND table_schema = 'public') THEN
        ALTER TABLE refresh_tokens ADD CONSTRAINT ukghpmfn23vmxfu3spu3lfg4r2d UNIQUE (token);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'ukkgh8ufue5vfb4t586cp3vb4hj' AND table_schema = 'public') THEN
        ALTER TABLE flight_statuses ADD CONSTRAINT ukkgh8ufue5vfb4t586cp3vb4hj UNIQUE (flight_id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'ukn2jopkbm16qv3xelbvoyjkd0g' AND table_schema = 'public') THEN
        ALTER TABLE notification_preferences ADD CONSTRAINT ukn2jopkbm16qv3xelbvoyjkd0g UNIQUE (user_id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'ukt4ffsaqe8d6i83gs100u2y3l1' AND table_schema = 'public') THEN
        ALTER TABLE payments ADD CONSTRAINT ukt4ffsaqe8d6i83gs100u2y3l1 UNIQUE (payment_id);
    END IF;
END $$;

-- ============================================================
-- BASE FOREIGN KEY CONSTRAINTS
-- ============================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk1lih5y2npsf8u5o3vhdb9y0os' AND table_schema = 'public') THEN
        ALTER TABLE refresh_tokens ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk1u0glmakf1bky242ovym6q8wi' AND table_schema = 'public') THEN
        ALTER TABLE flights ADD CONSTRAINT fk1u0glmakf1bky242ovym6q8wi FOREIGN KEY (destination_id) REFERENCES airports(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk28pfq0d7eeg115j61qoemusbo' AND table_schema = 'public') THEN
        ALTER TABLE fare_options ADD CONSTRAINT fk28pfq0d7eeg115j61qoemusbo FOREIGN KEY (flight_id) REFERENCES flights(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk2qgkqf0r6hmvgqq40t8daay6d' AND table_schema = 'public') THEN
        ALTER TABLE reward_transactions ADD CONSTRAINT fk2qgkqf0r6hmvgqq40t8daay6d FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk3fon902pd833mij0xrct29f2v' AND table_schema = 'public') THEN
        ALTER TABLE flight_status_history ADD CONSTRAINT fk3fon902pd833mij0xrct29f2v FOREIGN KEY (flight_id) REFERENCES flights(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk3mvslb8gc0ac6501mfmvifgva' AND table_schema = 'public') THEN
        ALTER TABLE coupon_usages ADD CONSTRAINT fk3mvslb8gc0ac6501mfmvifgva FOREIGN KEY (coupon_id) REFERENCES coupons(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk3nlfu8o4nsl1kcfp2sg8q4gnd' AND table_schema = 'public') THEN
        ALTER TABLE refunds ADD CONSTRAINT fk3nlfu8o4nsl1kcfp2sg8q4gnd FOREIGN KEY (booking_id) REFERENCES bookings(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk5bf4o4ap2v7g3qpxt8y28a38x' AND table_schema = 'public') THEN
        ALTER TABLE review_votes ADD CONSTRAINT fk5bf4o4ap2v7g3qpxt8y28a38x FOREIGN KEY (review_id) REFERENCES reviews(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk5v984hm7iyuvyccsgboplo7ii' AND table_schema = 'public') THEN
        ALTER TABLE hotel_amenities ADD CONSTRAINT fk5v984hm7iyuvyccsgboplo7ii FOREIGN KEY (hotel_id) REFERENCES hotels(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk6mev6grxbqmt8l0jxvobfg70n' AND table_schema = 'public') THEN
        ALTER TABLE coupon_usages ADD CONSTRAINT fk6mev6grxbqmt8l0jxvobfg70n FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk7y09f5lun38jnooaw2hch0ke9' AND table_schema = 'public') THEN
        ALTER TABLE bookings ADD CONSTRAINT fk7y09f5lun38jnooaw2hch0ke9 FOREIGN KEY (hotel_id) REFERENCES hotels(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk8qbyfo5qix0qqg5euuk81qo0p' AND table_schema = 'public') THEN
        ALTER TABLE seats ADD CONSTRAINT fk8qbyfo5qix0qqg5euuk81qo0p FOREIGN KEY (flight_id) REFERENCES flights(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk9y21adhxn0ayjhfocscqox7bh' AND table_schema = 'public') THEN
        ALTER TABLE notifications ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkb9igk5exfb4knqklcvka6cdhx' AND table_schema = 'public') THEN
        ALTER TABLE reviews ADD CONSTRAINT fkb9igk5exfb4knqklcvka6cdhx FOREIGN KEY (hotel_id) REFERENCES hotels(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkbtntnxygawfi1l6ppnaysyx4i' AND table_schema = 'public') THEN
        ALTER TABLE user_interactions ADD CONSTRAINT fkbtntnxygawfi1l6ppnaysyx4i FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkc0ikrxc3c5qg5v486378guyl0' AND table_schema = 'public') THEN
        ALTER TABLE reviews ADD CONSTRAINT fkc0ikrxc3c5qg5v486378guyl0 FOREIGN KEY (flight_id) REFERENCES flights(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkc52o2b1jkxttngufqp3t7jr3h' AND table_schema = 'public') THEN
        ALTER TABLE payments ADD CONSTRAINT fkc52o2b1jkxttngufqp3t7jr3h FOREIGN KEY (booking_id) REFERENCES bookings(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkcgy7qjc1r99dp117y9en6lxye' AND table_schema = 'public') THEN
        ALTER TABLE reviews ADD CONSTRAINT fkcgy7qjc1r99dp117y9en6lxye FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkcjycpchyf2q39d4f92xohf39b' AND table_schema = 'public') THEN
        ALTER TABLE flight_statuses ADD CONSTRAINT fkcjycpchyf2q39d4f92xohf39b FOREIGN KEY (flight_id) REFERENCES flights(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkeyog2oic85xg7hsu2je2lx3s6' AND table_schema = 'public') THEN
        ALTER TABLE bookings ADD CONSTRAINT fkeyog2oic85xg7hsu2je2lx3s6 FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkf48a6afo7lolirjks7uorfaah' AND table_schema = 'public') THEN
        ALTER TABLE review_votes ADD CONSTRAINT fkf48a6afo7lolirjks7uorfaah FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkfka09raqpeax3xpmxwxf8s95' AND table_schema = 'public') THEN
        ALTER TABLE seat_holds ADD CONSTRAINT fkfka09raqpeax3xpmxwxf8s95 FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkgnl29qtk74o194gp6fe4jto42' AND table_schema = 'public') THEN
        ALTER TABLE reward_accounts ADD CONSTRAINT fkgnl29qtk74o194gp6fe4jto42 FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkhpge7n8ekopmnunpqrakrkpbt' AND table_schema = 'public') THEN
        ALTER TABLE saved_preferences ADD CONSTRAINT fkhpge7n8ekopmnunpqrakrkpbt FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkidcytqkgq0ve4x1elcnbmdy8a' AND table_schema = 'public') THEN
        ALTER TABLE bookings ADD CONSTRAINT fkidcytqkgq0ve4x1elcnbmdy8a FOREIGN KEY (flight_id) REFERENCES flights(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkieor4j3ivp3xu584qenhfh0gd' AND table_schema = 'public') THEN
        ALTER TABLE flights ADD CONSTRAINT fkieor4j3ivp3xu584qenhfh0gd FOREIGN KEY (airline_id) REFERENCES airlines(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkj1yhywjt8ddf8kq9g6kvwcnbj' AND table_schema = 'public') THEN
        ALTER TABLE flights ADD CONSTRAINT fkj1yhywjt8ddf8kq9g6kvwcnbj FOREIGN KEY (origin_id) REFERENCES airports(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkk8ng2k5mf1sf7mpkiuk29p3ee' AND table_schema = 'public') THEN
        ALTER TABLE review_replies ADD CONSTRAINT fkk8ng2k5mf1sf7mpkiuk29p3ee FOREIGN KEY (review_id) REFERENCES reviews(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkke7enkercjikncvttgjh5cmnj' AND table_schema = 'public') THEN
        ALTER TABLE train_seats ADD CONSTRAINT fkke7enkercjikncvttgjh5cmnj FOREIGN KEY (train_id) REFERENCES trains(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkp5lufxy0ghq53ugm93hdc941k' AND table_schema = 'public') THEN
        ALTER TABLE rooms ADD CONSTRAINT fkp5lufxy0ghq53ugm93hdc941k FOREIGN KEY (hotel_id) REFERENCES hotels(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkpiii4vmtjq8vvvhko1eis61rx' AND table_schema = 'public') THEN
        ALTER TABLE review_replies ADD CONSTRAINT fkpiii4vmtjq8vvvhko1eis61rx FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkr79yscefm1uwts5gk328450no' AND table_schema = 'public') THEN
        ALTER TABLE saved_travellers ADD CONSTRAINT fkr79yscefm1uwts5gk328450no FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkrgoycol97o21kpjodw1qox4nc' AND table_schema = 'public') THEN
        ALTER TABLE bookings ADD CONSTRAINT fkrgoycol97o21kpjodw1qox4nc FOREIGN KEY (room_id) REFERENCES rooms(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkrj3n45f8oqy1yr996g14j757i' AND table_schema = 'public') THEN
        ALTER TABLE hotel_images ADD CONSTRAINT fkrj3n45f8oqy1yr996g14j757i FOREIGN KEY (hotel_id) REFERENCES hotels(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkskemaq4h19hyph1tysj6w4mun' AND table_schema = 'public') THEN
        ALTER TABLE coupon_usages ADD CONSTRAINT fkskemaq4h19hyph1tysj6w4mun FOREIGN KEY (booking_id) REFERENCES bookings(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fksqaitepj3whcje1a3hosqswld' AND table_schema = 'public') THEN
        ALTER TABLE seat_holds ADD CONSTRAINT fksqaitepj3whcje1a3hosqswld FOREIGN KEY (seat_id) REFERENCES seats(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkt9qjvmcl36i14utm5uptyqg84' AND table_schema = 'public') THEN
        ALTER TABLE notification_preferences ADD CONSTRAINT fkt9qjvmcl36i14utm5uptyqg84 FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fkunrlxq8kevetatdevbd9xbp1' AND table_schema = 'public') THEN
        ALTER TABLE review_photos ADD CONSTRAINT fkunrlxq8kevetatdevbd9xbp1 FOREIGN KEY (review_id) REFERENCES reviews(id);
    END IF;
END $$;

-- ============================================================
-- BASELINE INDEXES (Preserved from V1 baseline additions)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_booking_created ON bookings (created_at);
CREATE INDEX IF NOT EXISTS idx_booking_reference ON bookings (booking_reference);
CREATE INDEX IF NOT EXISTS idx_payment_booking ON payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payment_created ON payments (created_at);
CREATE INDEX IF NOT EXISTS idx_payment_razorpay_order ON payments (razorpay_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_razorpay_payment ON payments (razorpay_payment_id);
CREATE INDEX IF NOT EXISTS idx_refund_booking ON refunds (booking_id);
CREATE INDEX IF NOT EXISTS idx_refund_status ON refunds (status);
CREATE INDEX IF NOT EXISTS idx_refund_created ON refunds (created_at);
CREATE INDEX IF NOT EXISTS idx_refund_razorpay ON refunds (razorpay_refund_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON refresh_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_token_revoked ON refresh_tokens (revoked);
CREATE INDEX IF NOT EXISTS idx_notif_delivery ON notifications (delivery_status);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_logs (actor_email);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_target ON audit_logs (target_user_id);
CREATE INDEX IF NOT EXISTS idx_saved_traveller_user ON saved_travellers (user_id);
CREATE INDEX IF NOT EXISTS idx_coupon_active ON coupons (active);
CREATE INDEX IF NOT EXISTS idx_coupon_expiry ON coupons (expiry_date);
CREATE INDEX IF NOT EXISTS idx_webhook_created ON webhook_events (created_at);
CREATE INDEX IF NOT EXISTS idx_seat_flight ON seats (flight_id);
CREATE INDEX IF NOT EXISTS idx_seat_cabin ON seats (cabin_class);
CREATE INDEX IF NOT EXISTS idx_fare_flight ON fare_options (flight_id);
CREATE INDEX IF NOT EXISTS idx_review_hotel ON reviews (hotel_id);
CREATE INDEX IF NOT EXISTS idx_review_user ON reviews (user_id);
CREATE INDEX IF NOT EXISTS idx_review_status ON reviews (status);
CREATE INDEX IF NOT EXISTS idx_flight_status_flight ON flight_statuses (flight_id);
CREATE INDEX IF NOT EXISTS idx_seat_hold_seat ON seat_holds (seat_id);
CREATE INDEX IF NOT EXISTS idx_seat_hold_user ON seat_holds (user_id);
CREATE INDEX IF NOT EXISTS idx_seat_hold_expires ON seat_holds (expires_at);
CREATE INDEX IF NOT EXISTS idx_booking_status ON bookings (status);
CREATE INDEX IF NOT EXISTS idx_booking_type ON bookings (booking_type);
CREATE INDEX IF NOT EXISTS idx_booking_user ON bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_coupon_usage_booking ON coupon_usages (booking_id);
CREATE INDEX IF NOT EXISTS idx_coupon_usage_coupon ON coupon_usages (coupon_id);
CREATE INDEX IF NOT EXISTS idx_coupon_usage_user ON coupon_usages (user_id);
CREATE INDEX IF NOT EXISTS idx_flight_date ON flights (departure_date);
CREATE INDEX IF NOT EXISTS idx_flight_departure ON flights (departure_time);
CREATE INDEX IF NOT EXISTS idx_flight_route ON flights (origin_code, destination_code);
CREATE INDEX IF NOT EXISTS idx_hold_expiry ON seat_holds (expires_at);
CREATE INDEX IF NOT EXISTS idx_hold_seat ON seat_holds (seat_id);
CREATE INDEX IF NOT EXISTS idx_hold_user ON seat_holds (user_id);
CREATE INDEX IF NOT EXISTS idx_hotel_city ON hotels (city);
CREATE INDEX IF NOT EXISTS idx_hotel_star ON hotels (star_rating);
CREATE INDEX IF NOT EXISTS idx_interaction_entity ON user_interactions (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_interaction_user ON user_interactions (user_id);
CREATE INDEX IF NOT EXISTS idx_notif_user_created ON notifications (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_notif_user_read ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_review_flight ON reviews (flight_id);
CREATE INDEX IF NOT EXISTS idx_review_hotel ON reviews (hotel_id);
CREATE INDEX IF NOT EXISTS idx_review_rating ON reviews (rating);
CREATE INDEX IF NOT EXISTS idx_review_status ON reviews (status);
CREATE INDEX IF NOT EXISTS idx_review_user ON reviews (user_id);
CREATE INDEX IF NOT EXISTS idx_room_hotel ON rooms (hotel_id);
CREATE INDEX IF NOT EXISTS idx_room_type ON rooms (room_type);
CREATE INDEX IF NOT EXISTS idx_rt_booking ON reward_transactions (booking_reference);
CREATE INDEX IF NOT EXISTS idx_rt_type ON reward_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_rt_user ON reward_transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_saved_pref_user ON saved_preferences (user_id);
CREATE INDEX IF NOT EXISTS idx_seat_flight ON seats (flight_id);
CREATE INDEX IF NOT EXISTS idx_seat_held ON seats (held_until);