package com.hien.mvp.seat;

/** Thống kê ghế của 1 sự kiện, dùng cho trang quản trị. */
public record SeatStats(Long eventId, Long total, Long booked) {
}
