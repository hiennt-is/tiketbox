package com.hien.mvp.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

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

/** Các quy tắc nghiệp vụ của luồng giữ ghế → xác nhận → huỷ (chạy tuần tự). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingFlowTest {

	private static final long EVENT_ID = 1L;

	@Autowired BookingService bookingService;
	@Autowired BookingRepository bookingRepo;
	@Autowired SeatRepository seatRepo;
	@Autowired UserService userService;
	@Autowired JdbcTemplate jdbc;

	List<Seat> seats;
	Long alice;
	Long bob;

	@BeforeEach
	void setUp() {
		TestDb.reset(jdbc);
		seats = seatRepo.findByEventIdOrderByIdAsc(EVENT_ID);
		alice = userService.register("alice", "password123").getId();
		bob = userService.register("bob", "password123").getId();
	}

	@Test
	void hold_then_confirm_books_all_held_seats() {
		bookingService.hold(seat(0), alice);
		bookingService.hold(seat(1), alice);

		List<Booking> bookings = bookingService.confirm(EVENT_ID, alice);

		assertThat(bookings).hasSize(2);
		assertThat(status(0)).isEqualTo(SeatStatus.BOOKED);
		assertThat(status(1)).isEqualTo(SeatStatus.BOOKED);
	}

	@Test
	void confirming_twice_does_not_create_duplicate_bookings() {
		bookingService.hold(seat(0), alice);
		bookingService.confirm(EVENT_ID, alice);

		assertThatThrownBy(() -> bookingService.confirm(EVENT_ID, alice))
				.isInstanceOf(BookingException.class)
				.hasMessageContaining("chưa giữ ghế nào");
		assertThat(bookingRepo.count()).isEqualTo(1);
	}

	@Test
	void holding_same_seat_twice_is_harmless() {
		bookingService.hold(seat(0), alice);
		bookingService.hold(seat(0), alice);

		assertThat(status(0)).isEqualTo(SeatStatus.HELD);
	}

	@Test
	void seat_held_by_someone_else_cannot_be_taken() {
		bookingService.hold(seat(0), alice);

		assertThatThrownBy(() -> bookingService.hold(seat(0), bob)).isInstanceOf(SeatUnavailableException.class);
	}

	@Test
	void cannot_hold_more_than_the_limit() {
		for (int i = 0; i < 4; i++) {
			bookingService.hold(seat(i), alice);
		}

		assertThatThrownBy(() -> bookingService.hold(seat(4), alice))
				.isInstanceOf(BookingException.class)
				.hasMessageContaining("tối đa 4");
	}

	@Test
	void released_seat_can_be_held_by_others() {
		bookingService.hold(seat(0), alice);
		bookingService.release(seat(0), alice);

		bookingService.hold(seat(0), bob);
		assertThat(seatRepo.findById(seat(0)).orElseThrow().getHeldByUserId()).isEqualTo(bob);
	}

	@Test
	void cannot_release_someone_elses_hold() {
		bookingService.hold(seat(0), alice);
		bookingService.release(seat(0), bob); // không lỗi, nhưng cũng không có tác dụng

		assertThat(seatRepo.findById(seat(0)).orElseThrow().getHeldByUserId()).isEqualTo(alice);
	}

	@Test
	void expired_hold_cannot_be_confirmed_and_is_free_for_others() {
		bookingService.hold(seat(0), alice);
		TestDb.expireHold(jdbc, seat(0));

		assertThatThrownBy(() -> bookingService.confirm(EVENT_ID, alice))
				.isInstanceOf(BookingException.class)
				.hasMessageContaining("hết thời gian giữ");
		// Người khác lấy được ngay, không cần chờ job nhả ghế
		bookingService.hold(seat(0), bob);
	}

	@Test
	void job_releases_expired_holds() {
		bookingService.hold(seat(0), alice);
		TestDb.expireHold(jdbc, seat(0));

		bookingService.releaseExpiredHolds();

		Seat s = seatRepo.findById(seat(0)).orElseThrow();
		assertThat(s.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
		assertThat(s.getHeldByUserId()).isNull();
	}

	@Test
	void cancel_frees_the_seat_and_it_can_be_booked_again() {
		bookingService.hold(seat(0), alice);
		Booking booking = bookingService.confirm(EVENT_ID, alice).getFirst();

		bookingService.cancel(booking.getId(), alice, false);
		bookingService.cancel(booking.getId(), alice, false); // huỷ 2 lần: không lỗi

		assertThat(status(0)).isEqualTo(SeatStatus.AVAILABLE);
		bookingService.hold(seat(0), bob);
		bookingService.confirm(EVENT_ID, bob);
		assertThat(bookingRepo.countBySeatIdAndStatus(seat(0), BookingStatus.CONFIRMED)).isEqualTo(1);
	}

	@Test
	void user_cannot_cancel_someone_elses_booking_but_admin_can() {
		bookingService.hold(seat(0), alice);
		Booking booking = bookingService.confirm(EVENT_ID, alice).getFirst();

		assertThatThrownBy(() -> bookingService.cancel(booking.getId(), bob, false))
				.isInstanceOf(BookingException.class)
				.hasMessageContaining("Không tìm thấy");

		bookingService.cancel(booking.getId(), bob, true);
		assertThat(bookingRepo.findById(booking.getId()).orElseThrow().isCancelled()).isTrue();
	}

	private Long seat(int index) {
		return seats.get(index).getId();
	}

	private SeatStatus status(int index) {
		return seatRepo.findById(seat(index)).orElseThrow().getStatus();
	}

}
