CREATE TABLE events (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    start_time TIMESTAMPTZ  NOT NULL
);

CREATE TABLE seats (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT      NOT NULL REFERENCES events (id),
    seat_code  VARCHAR(10) NOT NULL,
    -- AVAILABLE | HELD | BOOKED
    status     VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    held_by    VARCHAR(100),
    held_until TIMESTAMPTZ,
    -- Dùng cho optimistic lock (@Version)
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_seat_event_code UNIQUE (event_id, seat_code)
);

CREATE TABLE bookings (
    id            BIGSERIAL PRIMARY KEY,
    seat_id       BIGINT       NOT NULL REFERENCES seats (id),
    customer_name VARCHAR(100) NOT NULL,
    -- Cách xử lý đã dùng khi tạo booking: NAIVE | DB_CONSTRAINT | PESSIMISTIC | OPTIMISTIC
    strategy      VARCHAR(20)  NOT NULL,
    -- CONFIRMED | CANCELLED
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_bookings_seat ON bookings (seat_id);
