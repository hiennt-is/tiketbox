package com.hien.mvp.booking;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 1 booking = 1 ghế đã đặt thành công của 1 người. */
@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "seat_id", nullable = false)
	private Long seatId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookingStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	public static Booking confirmed(Long seatId, Long userId) {
		Booking b = new Booking();
		b.seatId = seatId;
		b.userId = userId;
		b.status = BookingStatus.CONFIRMED;
		b.createdAt = Instant.now();
		return b;
	}

	public boolean isCancelled() {
		return status == BookingStatus.CANCELLED;
	}

	public void cancel(Instant now) {
		this.status = BookingStatus.CANCELLED;
		this.cancelledAt = now;
	}

}
