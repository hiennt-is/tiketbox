package com.hien.mvp.booking;

/** Người dùng "thua" trong cuộc đua giành ghế: ghế đã có người khác giữ hoặc đặt. */
public class SeatUnavailableException extends BookingException {

	public SeatUnavailableException(String seatCode) {
		super("Ghế " + seatCode + " vừa có người khác chọn. Hãy chọn ghế khác.");
	}

	public SeatUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}
