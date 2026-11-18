package com.hien.mvp.booking;

/** Lỗi nghiệp vụ khi đặt/giữ ghế, có message để hiển thị cho người dùng. */
public class BookingException extends RuntimeException {

	public BookingException(String message) {
		super(message);
	}

	public BookingException(String message, Throwable cause) {
		super(message, cause);
	}

}
