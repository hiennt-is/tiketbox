package com.hien.mvp.web;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hien.mvp.booking.Booking;
import com.hien.mvp.booking.BookingException;
import com.hien.mvp.booking.BookingRepository;
import com.hien.mvp.booking.BookingService;
import com.hien.mvp.booking.BookingStatus;
import com.hien.mvp.event.Event;
import com.hien.mvp.event.EventRepository;
import com.hien.mvp.seat.Seat;
import com.hien.mvp.seat.SeatRepository;
import com.hien.mvp.user.AppUserDetails;

/**
 * Danh sách sự kiện, sơ đồ ghế, và các thao tác giữ / bỏ giữ / xác nhận.
 * Mọi POST đều theo mẫu Post/Redirect/Get: xử lý xong thì redirect về trang ghế kèm flash message.
 */
@Controller
public class EventController {

	private final EventRepository eventRepo;
	private final SeatRepository seatRepo;
	private final BookingRepository bookingRepo;
	private final BookingService bookingService;
	private final int maxHeldSeats;

	public EventController(EventRepository eventRepo, SeatRepository seatRepo, BookingRepository bookingRepo,
			BookingService bookingService, @Value("${booking.max-held-seats:4}") int maxHeldSeats) {
		this.eventRepo = eventRepo;
		this.seatRepo = seatRepo;
		this.bookingRepo = bookingRepo;
		this.bookingService = bookingService;
		this.maxHeldSeats = maxHeldSeats;
	}

	@GetMapping("/")
	public String home() {
		return "redirect:/events";
	}

	@GetMapping("/events")
	public String events(Model model) {
		model.addAttribute("events", eventRepo.findAll().stream()
				.sorted(Comparator.comparing(Event::getStartTime)).toList());
		model.addAttribute("now", Instant.now());
		return "events";
	}

	@GetMapping("/events/{id}")
	public String seats(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me, Model model) {
		Event event = eventRepo.findById(id).orElseThrow(() -> new NotFoundException("Không có sự kiện #" + id));
		Instant now = Instant.now();
		Long viewerId = me == null ? null : me.id();

		List<Seat> seats = seatRepo.findByEventIdOrderByIdAsc(id);
		Map<Long, Long> bookedBy = bookingRepo
				.findBySeatIdInAndStatus(seats.stream().map(Seat::getId).toList(), BookingStatus.CONFIRMED)
				.stream().collect(Collectors.toMap(Booking::getSeatId, Booking::getUserId));
		List<SeatView> views = seats.stream()
				.map(s -> SeatView.of(s, viewerId, bookedBy.get(s.getId()), now))
				.toList();
		List<SeatView> myHolds = views.stream().filter(SeatView::mineHeld).toList();

		model.addAttribute("event", event);
		model.addAttribute("started", event.hasStarted(now));
		// TreeMap để các hàng A, B, C hiện đúng thứ tự
		model.addAttribute("rows", views.stream()
				.collect(Collectors.groupingBy(SeatView::row, TreeMap::new, Collectors.toList())));
		model.addAttribute("freeCount", views.stream().filter(SeatView::free).count());
		model.addAttribute("myHolds", myHolds);
		Instant holdExpiresAt = myHolds.stream().map(SeatView::heldUntil)
				.min(Comparator.naturalOrder()).orElse(null);
		model.addAttribute("holdExpiresAt", holdExpiresAt);
		// Thymeleaf không cho gọi bean trong th:attr, nên tính sẵn ở đây cho đồng hồ đếm ngược (JS)
		model.addAttribute("holdExpiresAtMillis", holdExpiresAt == null ? 0 : holdExpiresAt.toEpochMilli());
		model.addAttribute("maxHeldSeats", maxHeldSeats);
		return "seats";
	}

	@PostMapping("/events/{id}/holds")
	public String hold(@PathVariable Long id, @RequestParam Long seatId,
			@AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
		run(ra, () -> {
			var seat = bookingService.hold(seatId, me.id());
			return "Đang giữ ghế " + seat.getSeatCode() + ". Hãy xác nhận trước khi hết giờ.";
		});
		return "redirect:/events/" + id;
	}

	@PostMapping("/events/{id}/holds/release")
	public String release(@PathVariable Long id, @RequestParam Long seatId,
			@AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
		run(ra, () -> {
			bookingService.release(seatId, me.id());
			return "Đã bỏ giữ ghế";
		});
		return "redirect:/events/" + id;
	}

	@PostMapping("/events/{id}/confirm")
	public String confirm(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me,
			RedirectAttributes ra) {
		try {
			List<Booking> bookings = bookingService.confirm(id, me.id());
			ra.addFlashAttribute("success", "Đặt thành công " + bookings.size() + " vé!");
			return "redirect:/me/bookings";
		}
		catch (BookingException e) {
			ra.addFlashAttribute("error", e.getMessage());
			return "redirect:/events/" + id;
		}
	}

	private void run(RedirectAttributes ra, java.util.function.Supplier<String> action) {
		try {
			ra.addFlashAttribute("success", action.get());
		}
		catch (BookingException e) {
			ra.addFlashAttribute("error", e.getMessage());
		}
	}

}
