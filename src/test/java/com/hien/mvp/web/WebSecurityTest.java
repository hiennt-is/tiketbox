package com.hien.mvp.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.hien.mvp.TestDb;
import com.hien.mvp.TestcontainersConfiguration;
import com.hien.mvp.booking.BookingService;
import com.hien.mvp.seat.SeatRepository;
import com.hien.mvp.seat.SeatStatus;
import com.hien.mvp.user.AppUserDetails;
import com.hien.mvp.user.Role;
import com.hien.mvp.user.User;
import com.hien.mvp.user.UserRepository;
import com.hien.mvp.user.UserService;

/** Kiểm tra phân quyền, CSRF và render các trang (end-to-end qua MockMvc). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WebSecurityTest {

	private static final long EVENT_ID = 1L;

	@Autowired MockMvc mvc;
	@Autowired UserService userService;
	@Autowired UserRepository userRepo;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired BookingService bookingService;
	@Autowired SeatRepository seatRepo;
	@Autowired com.hien.mvp.event.EventRepository eventRepo;
	@Autowired JdbcTemplate jdbc;

	AppUserDetails alice;
	AppUserDetails boss;
	Long seatId;

	@BeforeEach
	void setUp() {
		TestDb.reset(jdbc);
		alice = AppUserDetails.from(userService.register("alice", "password123"));
		boss = AppUserDetails.from(userRepo.saveAndFlush(
				new User("boss", passwordEncoder.encode("password123"), Role.ADMIN)));
		seatId = seatRepo.findByEventIdOrderByIdAsc(EVENT_ID).getFirst().getId();
	}

	@Test
	void anonymous_can_browse_events_and_seats() throws Exception {
		mvc.perform(get("/events")).andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Concert")));
		mvc.perform(get("/events/" + EVENT_ID)).andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Đăng nhập để chọn ghế")));
	}

	@Test
	void anonymous_is_sent_to_login_when_holding_a_seat() throws Exception {
		mvc.perform(post("/events/" + EVENT_ID + "/holds").param("seatId", seatId.toString()).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
		mvc.perform(get("/me/bookings")).andExpect(redirectedUrl("/login"));
	}

	@Test
	void post_without_csrf_token_is_rejected() throws Exception {
		mvc.perform(post("/events/" + EVENT_ID + "/holds").param("seatId", seatId.toString()).with(user(alice)))
				.andExpect(status().isForbidden());
	}

	@Test
	void login_with_correct_and_wrong_password() throws Exception {
		mvc.perform(formLogin().user("alice").password("password123")).andExpect(authenticated());
		mvc.perform(formLogin().user("alice").password("wrong-pass")).andExpect(unauthenticated());
	}

	@Test
	void register_page_validates_and_creates_account() throws Exception {
		mvc.perform(post("/register").with(csrf())
				.param("username", "x").param("password", "short").param("confirmPassword", "other"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Từ 3 đến 30 ký tự")))
				.andExpect(content().string(Matchers.containsString("không khớp")));

		mvc.perform(post("/register").with(csrf())
				.param("username", "alice").param("password", "password123").param("confirmPassword", "password123"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("đã có người dùng")));

		mvc.perform(post("/register").with(csrf())
				.param("username", "newbie").param("password", "password123").param("confirmPassword", "password123"))
				.andExpect(redirectedUrl("/login?registered"));
		assertThat(userRepo.findByUsername("newbie")).isPresent();
	}

	@Test
	void logged_in_user_can_hold_confirm_and_see_their_ticket() throws Exception {
		mvc.perform(post("/events/" + EVENT_ID + "/holds").param("seatId", seatId.toString())
				.with(user(alice)).with(csrf()))
				.andExpect(redirectedUrl("/events/" + EVENT_ID));
		assertThat(seatRepo.findById(seatId).orElseThrow().getHeldByUserId()).isEqualTo(alice.id());

		mvc.perform(get("/events/" + EVENT_ID).with(user(alice)))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Xác nhận đặt 1 vé")));

		mvc.perform(post("/events/" + EVENT_ID + "/confirm").with(user(alice)).with(csrf()))
				.andExpect(redirectedUrl("/me/bookings"));
		assertThat(seatRepo.findById(seatId).orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);

		mvc.perform(get("/me/bookings").with(user(alice)))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Huỷ vé")));
	}

	@Test
	void only_admin_can_open_admin_pages() throws Exception {
		bookingService.hold(seatId, alice.id());
		bookingService.confirm(EVENT_ID, alice.id());

		mvc.perform(get("/admin/bookings").with(user(alice))).andExpect(status().isForbidden());
		mvc.perform(get("/admin/bookings").with(user(boss)))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("alice")));
	}

	@Test
	void only_admin_can_create_events() throws Exception {
		mvc.perform(get("/admin/events/new").with(user(alice))).andExpect(status().isForbidden());
		mvc.perform(post("/admin/events").with(user(alice)).with(csrf())
				.param("name", "Hack").param("startTime", "2099-01-01T20:00")
				.param("rows", "1").param("seatsPerRow", "1"))
				.andExpect(status().isForbidden());
		assertThat(eventRepo.count()).isEqualTo(2);

		mvc.perform(get("/admin/events").with(user(boss))).andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Concert")));
		mvc.perform(get("/admin/events/new").with(user(boss))).andExpect(status().isOk());
	}

	@Test
	void event_form_shows_validation_errors() throws Exception {
		mvc.perform(post("/admin/events").with(user(boss)).with(csrf())
				.param("name", "").param("startTime", "2020-01-01T20:00")
				.param("rows", "0").param("seatsPerRow", "31"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Không được để trống")))
				.andExpect(content().string(Matchers.containsString("phải ở tương lai")))
				.andExpect(content().string(Matchers.containsString("Ít nhất 1 hàng")))
				.andExpect(content().string(Matchers.containsString("Tối đa 30 ghế")));
		assertThat(eventRepo.count()).isEqualTo(2);
	}

	@Test
	void admin_creates_event_and_users_can_book_it() throws Exception {
		mvc.perform(post("/admin/events").with(user(boss)).with(csrf())
				.param("name", "Đêm nhạc Test").param("startTime", "2099-06-01T20:00")
				.param("rows", "2").param("seatsPerRow", "4"))
				.andExpect(redirectedUrl("/admin/events"));

		var event = eventRepo.findAll().stream().filter(e -> e.getName().equals("Đêm nhạc Test")).findFirst()
				.orElseThrow();
		// 20:00 giờ VN = 13:00 UTC
		assertThat(event.getStartTime()).isEqualTo(java.time.Instant.parse("2099-06-01T13:00:00Z"));
		mvc.perform(get("/events")).andExpect(content().string(Matchers.containsString("Đêm nhạc Test")));

		// Gửi lại đúng form đó (bấm 2 lần) thì bị báo trùng
		mvc.perform(post("/admin/events").with(user(boss)).with(csrf())
				.param("name", "Đêm nhạc Test").param("startTime", "2099-06-01T20:00")
				.param("rows", "2").param("seatsPerRow", "4"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Đã có sự kiện")));

		Long newSeat = seatRepo.findByEventIdOrderByIdAsc(event.getId()).getFirst().getId();
		bookingService.hold(newSeat, alice.id());
		assertThat(bookingService.confirm(event.getId(), alice.id())).hasSize(1);
	}

}
