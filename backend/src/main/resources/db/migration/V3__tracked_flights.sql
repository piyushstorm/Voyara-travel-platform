CREATE TABLE tracked_flights (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flight_id BIGINT NOT NULL,
    tracked_at TIMESTAMP NOT NULL,
    notifications_enabled BOOLEAN DEFAULT true,
    CONSTRAINT fk_tracked_flights_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tracked_flights_flight FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE,
    CONSTRAINT uq_tracked_flights_user_flight UNIQUE (user_id, flight_id)
);

CREATE INDEX idx_tracked_flights_user ON tracked_flights(user_id);
