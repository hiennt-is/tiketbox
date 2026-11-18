package com.hien.mvp.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Định kỳ nhả các ghế đã giữ mà quá hạn chưa xác nhận (người dùng bỏ ngang). */
@Component
class HoldExpiryJob {

	private static final Logger log = LoggerFactory.getLogger(HoldExpiryJob.class);

	private final BookingService bookingService;

	HoldExpiryJob(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@Scheduled(fixedRateString = "${booking.hold-release-interval:30s}")
	void releaseExpiredHolds() {
		int released = bookingService.releaseExpiredHolds();
		if (released > 0) {
			log.info("Đã nhả {} ghế hết hạn giữ", released);
		}
	}

}
