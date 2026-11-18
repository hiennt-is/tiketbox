package com.hien.mvp.booking;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hien.mvp.event.Event;
import com.hien.mvp.event.EventRepository;
import com.hien.mvp.seat.Seat;
import com.hien.mvp.seat.SeatRepository;
import com.hien.mvp.seat.SeatStatus;

/**
 * Mỗi method public = 1 transaction. TẤT CẢ đều dùng OPTIMISTIC LOCK:
 * đọc bình thường (không khóa), kiểm tra, rồi ghi bằng {@code saveAndFlush}. Hibernate tự thêm
 * {@code WHERE version = ?}, nên nếu ai đó đã ghi ghế này trước thì sẽ có
 * {@code ObjectOptimisticLockingFailureException} và toàn bộ transaction rollback.
 *
 * <p>Không gọi trực tiếp class này; hãy gọi qua {@link BookingService}, nơi bắt các lỗi đó
 * (có thể bị ném ra cả lúc COMMIT, tức là sau khi method ở đây đã return).
 */
@Component
class BookingTransactions {

	private final SeatRepository seatRepo;
	private final BookingRepository bookingRepo;
	private final EventRepository eventRepo;
	private final Duration holdDuration;
	private final int maxHeldSeats;

	BookingTransactions(SeatRepository seatRepo, BookingRepository bookingRepo, EventRepository eventRepo,
			@Value("${booking.hold-duration:5m}") Duration holdDuration,
			@Value("${booking.max-held-seats:4}") int maxHeldSeats) {
		this.seatRepo = seatRepo;
		this.bookingRepo = bookingRepo;
		this.eventRepo = eventRepo;
		this.holdDuration = holdDuration;
		this.maxHeldSeats = maxHeldSeats;
	}

	// ------------------------------------------------------------------ GIỮ GHẾ
	@Transactional
	public Seat hold(Long seatId, Long userId) {
		Instant now = Instant.now();
		Seat seat = loadSeat(seatId);                                   // 1. ĐỌC (nhớ version)
		ensureNotStarted(seat.getEventId(), now);

		if (seat.isHeldBy(userId, now)) {                               // bấm 2 lần: không lỗi
			return seat;
		}
		if (!seat.isFree(now)) {                                        // 2. KIỂM TRA
			throw new SeatUnavailableException(seat.getSeatCode());
		}
		// Ghi chú: 2 request giữ 2 ghế KHÁC NHAU của cùng 1 người chạy song song có thể cùng
		// vượt qua bước đếm này (giữ được max+1 ghế). Chấp nhận được với app nhỏ; muốn chặt
		// thì phải khóa theo user (ví dụ thêm @Version vào User và cập nhật nó ở đây).
		if (seatRepo.countActiveHolds(seat.getEventId(), userId, now) >= maxHeldSeats) {
			throw new BookingException("Mỗi người chỉ được giữ tối đa " + maxHeldSeats + " ghế");
		}

		seat.markHeld(userId, now.plus(holdDuration));
		return seatRepo.saveAndFlush(seat);                             // 3. GHI ... WHERE version = ?
	}

	// ------------------------------------------------------------------ BỎ GIỮ
	/** Idempotent: ghế không còn do mình giữ (đã hết hạn, đã bỏ) thì không làm gì. */
	@Transactional
	public void release(Long seatId, Long userId) {
		Seat seat = loadSeat(seatId);
		if (seat.getStatus() == SeatStatus.HELD && userId.equals(seat.getHeldByUserId())) {
			seat.markAvailable();
			seatRepo.saveAndFlush(seat);
		}
	}

	// ------------------------------------------------------------------ XÁC NHẬN
	/**
	 * Chuyển TẤT CẢ ghế mình đang giữ trong sự kiện thành booking, trong 1 transaction:
	 * hoặc đặt được hết, hoặc không ghế nào (không có chuyện đặt được 2/3 ghế).
	 *
	 * <p>Nếu đúng lúc này job nhả ghế hết hạn chạy, nó tăng version của ghế, nên lệnh ghi ở đây
	 * bị lỗi optimistic lock và rollback toàn bộ. Không bao giờ có chuyện vừa nhả vừa đặt.
	 */
	@Transactional
	public List<Booking> confirm(Long eventId, Long userId) {
		Instant now = Instant.now();
		ensureNotStarted(eventId, now);

		List<Seat> seats = seatRepo.findByEventIdAndStatusAndHeldByUserIdOrderByIdAsc(eventId, SeatStatus.HELD,
				userId);
		if (seats.isEmpty()) {
			throw new BookingException("Bạn chưa giữ ghế nào (hoặc đã hết thời gian giữ)");
		}
		for (Seat s : seats) {
			if (!s.isHeldBy(userId, now)) {
				throw new BookingException("Ghế " + s.getSeatCode() + " đã hết thời gian giữ. Hãy chọn lại.");
			}
			s.markBooked();
		}
		seatRepo.saveAllAndFlush(seats);                                // WHERE version = ? cho từng ghế
		// Unique index ux_booking_seat_confirmed vẫn là tuyến phòng thủ cuối ở DB
		return bookingRepo.saveAllAndFlush(seats.stream().map(s -> Booking.confirmed(s.getId(), userId)).toList());
	}

	// ------------------------------------------------------------------ HUỶ VÉ
	/**
	 * Huỷ booking, ghế quay về AVAILABLE. Người dùng chỉ huỷ được vé của mình và trước giờ diễn;
	 * admin huỷ được mọi vé. Huỷ 2 lần thì lần sau không làm gì (idempotent).
	 */
	@Transactional
	public Booking cancel(Long bookingId, Long userId, boolean admin) {
		Instant now = Instant.now();
		Booking booking = bookingRepo.findById(bookingId)
				.filter(b -> admin || b.getUserId().equals(userId))     // không phải của mình = coi như không có
				.orElseThrow(() -> new BookingException("Không tìm thấy vé #" + bookingId));
		if (booking.isCancelled()) {
			return booking;
		}
		Seat seat = loadSeat(booking.getSeatId());
		if (!admin) {
			ensureNotStarted(seat.getEventId(), now);
		}
		booking.cancel(now);
		seat.markAvailable();
		seatRepo.saveAndFlush(seat);                                    // 2 lệnh huỷ song song: 1 lệnh thua version
		return booking;
	}

	// ------------------------------------------------------------------ helpers
	private Seat loadSeat(Long seatId) {
		return seatRepo.findById(seatId).orElseThrow(() -> new BookingException("Không tìm thấy ghế #" + seatId));
	}

	private void ensureNotStarted(Long eventId, Instant now) {
		Event event = eventRepo.findById(eventId)
				.orElseThrow(() -> new BookingException("Không tìm thấy sự kiện #" + eventId));
		if (event.hasStarted(now)) {
			throw new BookingException("Sự kiện đã bắt đầu, không thể thay đổi vé");
		}
	}

}
