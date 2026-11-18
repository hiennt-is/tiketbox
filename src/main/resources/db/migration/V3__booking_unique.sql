-- Tuyến phòng thủ cuối cùng: mỗi ghế chỉ có tối đa 1 booking CONFIRMED.
-- Dù code ứng dụng có bug, DB vẫn từ chối booking thứ hai.
--
-- LƯU Ý (chỉ để học): index cố tình BỎ QUA các booking tạo bằng strategy NAIVE,
-- để vẫn có thể tái hiện lỗi double-booking. Hệ thống thật thì bỏ điều kiện strategy đi.
CREATE UNIQUE INDEX ux_booking_seat_confirmed
    ON bookings (seat_id)
    WHERE status = 'CONFIRMED' AND strategy <> 'NAIVE';
