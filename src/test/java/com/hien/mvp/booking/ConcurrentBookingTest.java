package com.hien.mvp.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.hien.mvp.TestDb;
import com.hien.mvp.TestcontainersConfiguration;
import com.hien.mvp.seat.Seat;
import com.hien.mvp.seat.SeatRepository;
import com.hien.mvp.seat.SeatStatus;
import com.hien.mvp.user.UserService;

/**
 * Nhiều request chạy CÙNG LÚC. Optimistic lock phải đảm bảo không bao giờ có 2 người
 * cùng giữ hoặc cùng đặt 1 ghế.
 *
 * <p>Class test KHÔNG có @Transactional: mỗi thread cần transaction riêng, giống request HTTP thật.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentBookingTest {

	private static final long EVENT_ID = 2L;
	private static final int USERS = 10;

	@Autowired BookingService bookingService;
	@Autowired BookingRepository bookingRepo;
	@Autowired SeatRepository seatRepo;
	@Autowired UserService userService;
	@Autowired JdbcTemplate jdbc;

	Long seatId;
	List<Long> users = new ArrayList<>();

	@BeforeEach
	void setUp() {
		TestDb.reset(jdbc);
		seatId = seatRepo.findByEventIdOrderByIdAsc(EVENT_ID).getFirst().getId();
		users.clear();
		for (int i = 0; i < USERS; i++) {
			users.add(userService.register("racer" + i, "password123").getId());
		}
	}

	@Test
	void ten_users_hold_the_same_seat_only_one_wins() throws Exception {
		List<String> results = runConcurrently(USERS, i -> () -> {
			bookingService.hold(seatId, users.get(i));
			return "OK";
		});

		assertThat(results).containsOnly("OK", "SeatUnavailableException");
		assertThat(results).filteredOn("OK"::equals).hasSize(1);

		Seat seat = seatRepo.findById(seatId).orElseThrow();
		assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
		assertThat(users).contains(seat.getHeldByUserId());
	}

	@Test
	void double_clicking_confirm_books_the_seat_only_once() throws Exception {
		Long alice = users.getFirst();
		bookingService.hold(seatId, alice);

		// Cùng 1 người bấm "Xác nhận" 5 lần liên tiếp (5 request song song)
		List<String> results = runConcurrently(5, i -> () -> {
			bookingService.confirm(EVENT_ID, alice);
			return "OK";
		});

		assertThat(results).filteredOn("OK"::equals).hasSize(1);
		assertThat(bookingRepo.countBySeatIdAndStatus(seatId, BookingStatus.CONFIRMED)).isEqualTo(1);
	}

	@Test
	void concurrent_cancels_free_the_seat_exactly_once() throws Exception {
		Long alice = users.getFirst();
		bookingService.hold(seatId, alice);
		Long bookingId = bookingService.confirm(EVENT_ID, alice).getFirst().getId();

		List<String> results = runConcurrently(5, i -> () -> {
			bookingService.cancel(bookingId, alice, false);
			return "OK";
		});

		assertThat(results).doesNotContain("ERROR"); // chỉ có OK hoặc lỗi tranh chấp đã được dịch
		assertThat(bookingRepo.findById(bookingId).orElseThrow().isCancelled()).isTrue();
		assertThat(seatRepo.findById(seatId).orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
	}

	/**
	 * Cho {@code n} thread cùng xuất phát (CountDownLatch) và trả về kết quả của từng thread:
	 * "OK", tên class của BookingException, hoặc "ERROR" nếu gặp lỗi không mong đợi.
	 */
	private List<String> runConcurrently(int n, java.util.function.IntFunction<Callable<String>> task)
			throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		List<Future<String>> futures = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(n)) {
			for (int i = 0; i < n; i++) {
				Callable<String> body = task.apply(i);
				futures.add(pool.submit(() -> {
					start.await();
					try {
						return body.call();
					}
					catch (BookingException e) {
						return e.getClass().getSimpleName();
					}
					catch (Exception e) {
						e.printStackTrace();
						return "ERROR";
					}
				}));
			}
			start.countDown();
		}
		List<String> results = new ArrayList<>();
		for (Future<String> f : futures) {
			results.add(f.get());
		}
		return results;
	}

}
