package com.hien.mvp.web;

import java.time.Instant;

import com.hien.mvp.seat.Seat;
import com.hien.mvp.seat.SeatStatus;

/** Ghế nhìn từ góc của người đang xem trang (ghế của tôi / của người khác / trống). */
public record SeatView(Long id, String code, State state, Instant heldUntil) {

	public enum State {
		FREE, MINE_HELD, MINE_BOOKED, HELD, BOOKED
	}

	/**
	 * @param viewerId   người đang xem (null nếu chưa đăng nhập)
	 * @param bookedById chủ booking CONFIRMED của ghế (null nếu chưa ai đặt)
	 */
	public static SeatView of(Seat seat, Long viewerId, Long bookedById, Instant now) {
		State state;
		if (seat.getStatus() == SeatStatus.BOOKED) {
			state = viewerId != null && viewerId.equals(bookedById) ? State.MINE_BOOKED : State.BOOKED;
		}
		else if (seat.isFree(now)) {
			state = State.FREE; // gồm cả ghế HELD đã hết hạn mà job chưa kịp nhả
		}
		else {
			state = viewerId != null && seat.isHeldBy(viewerId, now) ? State.MINE_HELD : State.HELD;
		}
		return new SeatView(seat.getId(), seat.getSeatCode(), state,
				state == State.MINE_HELD ? seat.getHeldUntil() : null);
	}

	public String row() {
		return code.substring(0, 1);
	}

	public boolean free() {
		return state == State.FREE;
	}

	public boolean mineHeld() {
		return state == State.MINE_HELD;
	}

}
