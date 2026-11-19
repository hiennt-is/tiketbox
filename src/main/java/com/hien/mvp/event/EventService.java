package com.hien.mvp.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.hien.mvp.seat.Seat;
import com.hien.mvp.seat.SeatRepository;

@Service
public class EventService {

	public static final int MAX_ROWS = 26;          // hàng A..Z
	public static final int MAX_SEATS_PER_ROW = 30;

	private final EventRepository eventRepo;
	private final SeatRepository seatRepo;
	private final TransactionTemplate tx;

	public EventService(EventRepository eventRepo, SeatRepository seatRepo, PlatformTransactionManager txManager) {
		this.eventRepo = eventRepo;
		this.seatRepo = seatRepo;
		this.tx = new TransactionTemplate(txManager);
	}

	/**
	 * Tạo sự kiện kèm sơ đồ ghế {@code rows} x {@code seatsPerRow} (A1..A10, B1..B10, ...).
	 *
	 * <p>Sự kiện và toàn bộ ghế được lưu trong 1 transaction: lỗi giữa chừng thì không có sự kiện
	 * nào bị thiếu ghế.
	 *
	 * <p>Dùng {@link TransactionTemplate} thay cho {@code @Transactional} để try/catch nằm NGOÀI
	 * transaction: lỗi vi phạm unique (tạo trùng) có thể chỉ xuất hiện lúc commit. Đây là cùng
	 * ý tưởng với việc tách BookingService / BookingTransactions.
	 */
	public Event create(String name, Instant startTime, int rows, int seatsPerRow) {
		if (rows < 1 || rows > MAX_ROWS || seatsPerRow < 1 || seatsPerRow > MAX_SEATS_PER_ROW) {
			throw new IllegalArgumentException("Sơ đồ ghế không hợp lệ: " + rows + " x " + seatsPerRow);
		}
		String trimmed = name.trim();
		try {
			return tx.execute(status -> {
				Event event = eventRepo.saveAndFlush(new Event(trimmed, startTime));
				List<Seat> seats = new ArrayList<>(rows * seatsPerRow);
				for (int r = 0; r < rows; r++) {
					char row = (char) ('A' + r);
					for (int n = 1; n <= seatsPerRow; n++) {
						seats.add(Seat.available(event.getId(), row + String.valueOf(n)));
					}
				}
				seatRepo.saveAll(seats);
				return event;
			});
		}
		catch (DataIntegrityViolationException e) {
			throw new DuplicateEventException(trimmed);
		}
	}

}
