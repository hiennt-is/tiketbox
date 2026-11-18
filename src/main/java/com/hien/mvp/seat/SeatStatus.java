package com.hien.mvp.seat;

/**
 * State machine của ghế:
 *
 * <pre>
 * AVAILABLE --giữ ghế--> HELD --xác nhận--> BOOKED
 *     ^                    |                  |
 *     +--bỏ giữ / hết hạn--+                  |
 *     +-------------------huỷ vé--------------+
 * </pre>
 */
public enum SeatStatus {
	AVAILABLE, HELD, BOOKED
}
