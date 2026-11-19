package com.hien.mvp.seat;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ghế của một sự kiện. Mọi thay đổi trạng thái đi qua các method {@code markXxx()}
 * để đảm bảo các cột luôn nhất quán (khớp với CHECK constraint ở V4).
 */
@Entity
@Table(name = "seats")
@Getter
@NoArgsConstructor
public class Seat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false)
	private Long eventId;

	@Column(name = "seat_code", nullable = false)
	private String seatCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SeatStatus status;

	@Column(name = "held_by_user_id")
	private Long heldByUserId;

	@Column(name = "held_until")
	private Instant heldUntil;

	/**
	 * OPTIMISTIC LOCK. Mỗi lần Hibernate ghi ghế này, nó chạy
	 * {@code UPDATE seats SET ..., version = version + 1 WHERE id = ? AND version = ?}.
	 * Nếu người khác đã ghi trước thì version không còn khớp, 0 dòng được cập nhật, và
	 * Hibernate ném {@code OptimisticLockException}: người đến sau "thua" và transaction bị rollback.
	 */
	@Version
	@Column(nullable = false)
	private Long version;

	/** Ghế mới, còn trống. {@code version} để null, Hibernate tự đặt 0 khi lưu. */
	public static Seat available(Long eventId, String seatCode) {
		Seat s = new Seat();
		s.eventId = eventId;
		s.seatCode = seatCode;
		s.status = SeatStatus.AVAILABLE;
		return s;
	}

	/** Ghế đang trống, hoặc đã từng bị giữ nhưng hết hạn thì coi như trống. */
	public boolean isFree(Instant now) {
		return switch (status) {
			case AVAILABLE -> true;
			case HELD -> heldUntil.isBefore(now);
			case BOOKED -> false;
		};
	}

	/** Ghế đang được {@code userId} giữ và chưa hết hạn. */
	public boolean isHeldBy(Long userId, Instant now) {
		return status == SeatStatus.HELD && userId.equals(heldByUserId) && !heldUntil.isBefore(now);
	}

	public void markHeld(Long userId, Instant until) {
		this.status = SeatStatus.HELD;
		this.heldByUserId = userId;
		this.heldUntil = until;
	}

	public void markBooked() {
		this.status = SeatStatus.BOOKED;
		this.heldByUserId = null;
		this.heldUntil = null;
	}

	public void markAvailable() {
		this.status = SeatStatus.AVAILABLE;
		this.heldByUserId = null;
		this.heldUntil = null;
	}

}
