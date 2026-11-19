package com.hien.mvp.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EventServiceTest {

	@Autowired EventService eventService;
	@Autowired EventRepository eventRepo;
	@Autowired SeatRepository seatRepo;
	@Autowired JdbcTemplate jdbc;

	Instant nextWeek = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);

	@BeforeEach
	void setUp() {
		TestDb.reset(jdbc);
	}

	@Test
	void creates_event_with_generated_seat_map() {
		Event event = eventService.create("  Live Show  ", nextWeek, 3, 5);

		assertThat(event.getName()).isEqualTo("Live Show");
		List<Seat> seats = seatRepo.findByEventIdOrderByIdAsc(event.getId());
		assertThat(seats).hasSize(15);
		assertThat(seats).extracting(Seat::getSeatCode)
				.containsExactly("A1", "A2", "A3", "A4", "A5", "B1", "B2", "B3", "B4", "B5",
						"C1", "C2", "C3", "C4", "C5");
		assertThat(seats).allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE && s.getVersion() == 0L);
	}

	@Test
	void duplicate_event_is_rejected() {
		eventService.create("Live Show", nextWeek, 2, 2);
		long before = eventRepo.count();

		assertThatThrownBy(() -> eventService.create("Live Show", nextWeek, 2, 2))
				.isInstanceOf(DuplicateEventException.class);
		assertThat(eventRepo.count()).isEqualTo(before);
	}

	@Test
	void invalid_seat_map_is_rejected() {
		assertThatThrownBy(() -> eventService.create("Big", nextWeek, 27, 10))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> eventService.create("Big", nextWeek, 3, 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void creating_the_same_event_five_times_at_once_creates_it_once() throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		List<Future<String>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(5)) {
			for (int i = 0; i < 5; i++) {
				results.add(pool.submit(() -> {
					start.await();
					try {
						eventService.create("Flash Concert", nextWeek, 2, 3);
						return "OK";
					}
					catch (DuplicateEventException e) {
						return "DUPLICATE";
					}
				}));
			}
			start.countDown();
		}
		List<String> outcomes = new ArrayList<>();
		for (Future<String> f : results) {
			outcomes.add(f.get());
		}

		assertThat(outcomes).filteredOn("OK"::equals).hasSize(1);
		assertThat(outcomes).filteredOn("DUPLICATE"::equals).hasSize(4);
		// Không có sự kiện "mồ côi" thiếu ghế: chỉ đúng 6 ghế của sự kiện thắng
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM seats s JOIN events e ON e.id = s.event_id WHERE e.name = 'Flash Concert'",
				Long.class)).isEqualTo(6L);
	}

}
