package com.hien.mvp.booking;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hien.mvp.seat.Seat;
import com.hien.mvp.seat.SeatRepository;

/**
 * Cửa ngõ nghiệp vụ đặt vé. Gọi {@link BookingTransactions} rồi đổi lỗi tranh chấp
 * (lỗi kỹ thuật) thành {@link BookingException} có câu thông báo dễ hiểu cho người dùng.
 *
 * <p>Vì sao phải tách 2 class? Lỗi optimistic lock hoặc vi phạm unique có thể được ném ra lúc
 * COMMIT, mà commit xảy ra ở proxy, SAU khi method @Transactional đã return. Chỉ đứng ở ngoài
 * transaction như class này mới bắt được.
 */
@Service
public class BookingService {

	private final BookingTransactions tx;
	private final SeatRepository seatRepo;

	public BookingService(BookingTransactions tx, SeatRepository seatRepo) {
		this.tx = tx;
		this.seatRepo = seatRepo;
	}

	public Seat hold(Long seatId, Long userId) {
		return guard(() -> tx.hold(seatId, userId),
				"Ghế vừa có người khác chọn. Hãy chọn ghế khác.");
	}

	public void release(Long seatId, Long userId) {
		guard(() -> {
			tx.release(seatId, userId);
			return null;
		}, "Ghế vừa thay đổi trạng thái, vui lòng tải lại trang.");
	}

	public List<Booking> confirm(Long eventId, Long userId) {
		return guard(() -> tx.confirm(eventId, userId),
				"Có ghế vừa hết thời gian giữ hoặc thay đổi trạng thái. Chưa có ghế nào được đặt, hãy kiểm tra lại.");
	}

	public Booking cancel(Long bookingId, Long userId, boolean admin) {
		return guard(() -> tx.cancel(bookingId, userId, admin),
				"Vé vừa được thay đổi ở nơi khác, vui lòng tải lại trang.");
	}

	@Transactional
	public int releaseExpiredHolds() {
		return seatRepo.releaseExpiredHolds(Instant.now());
	}

	private <T> T guard(Supplier<T> action, String conflictMessage) {
		try {
			return action.get();
		}
		catch (OptimisticLockingFailureException   // @Version không khớp: có người ghi trước
				| DataIntegrityViolationException e) { // unique index chặn booking trùng
			throw new SeatUnavailableException(conflictMessage, e);
		}
	}

}
