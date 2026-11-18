package com.hien.mvp.booking;

import java.time.Instant;

/** Dữ liệu hiển thị 1 booking (join từ bookings + seats + events + users), chỉ để đọc. */
public record BookingView(
		Long id,
		Long eventId,
		String eventName,
		Instant eventStart,
		String seatCode,
		String username,
		BookingStatus status,
		Instant createdAt,
		Instant cancelledAt) {

	public boolean cancellable(Instant now) {
		return status == BookingStatus.CONFIRMED && eventStart.isAfter(now);
	}

}
