-- =====================================================================
-- V4: thêm tài khoản người dùng, bỏ chế độ NAIVE/lab, chỉ dùng optimistic lock.
-- Không sửa V1-V3 vì đã chạy trên DB: Flyway lưu checksum, sửa file cũ sẽ báo lỗi.
-- =====================================================================

-- Dữ liệu cũ chỉ là dữ liệu thí nghiệm (có cả booking trùng do NAIVE) nên xoá sạch.
DELETE FROM bookings;
UPDATE seats SET status = 'AVAILABLE', held_by = NULL, held_until = NULL;

-- ---------------------------------------------------------------- users
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    -- UNIQUE: chặn 2 người đăng ký trùng username cùng lúc (race condition khi đăng ký)
    username      VARCHAR(30)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- seats
ALTER TABLE seats DROP COLUMN held_by;
ALTER TABLE seats ADD COLUMN held_by_user_id BIGINT REFERENCES users (id);
ALTER TABLE seats ADD CONSTRAINT ck_seat_status CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED'));
-- Ghế HELD bắt buộc có người giữ + hạn giữ; ghế khác thì không được có
ALTER TABLE seats ADD CONSTRAINT ck_seat_hold_consistent CHECK (
    (status = 'HELD') = (held_by_user_id IS NOT NULL AND held_until IS NOT NULL)
);
CREATE INDEX ix_seats_holder ON seats (held_by_user_id) WHERE held_by_user_id IS NOT NULL;

-- ------------------------------------------------------------- bookings
DROP INDEX ux_booking_seat_confirmed;
ALTER TABLE bookings DROP COLUMN strategy;
ALTER TABLE bookings DROP COLUMN customer_name;
ALTER TABLE bookings ADD COLUMN user_id BIGINT NOT NULL REFERENCES users (id);
ALTER TABLE bookings ADD COLUMN cancelled_at TIMESTAMPTZ;
ALTER TABLE bookings ADD CONSTRAINT ck_booking_status CHECK (status IN ('CONFIRMED', 'CANCELLED'));
CREATE INDEX ix_bookings_user ON bookings (user_id);

-- Tuyến phòng thủ cuối: mỗi ghế tối đa 1 booking CONFIRMED. Áp dụng cho MỌI booking.
CREATE UNIQUE INDEX ux_booking_seat_confirmed ON bookings (seat_id) WHERE status = 'CONFIRMED';
