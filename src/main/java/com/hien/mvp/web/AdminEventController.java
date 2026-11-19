package com.hien.mvp.web;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hien.mvp.event.DuplicateEventException;
import com.hien.mvp.event.Event;
import com.hien.mvp.event.EventRepository;
import com.hien.mvp.event.EventService;
import com.hien.mvp.seat.SeatRepository;
import com.hien.mvp.seat.SeatStats;

/** Quản lý sự kiện. Chỉ ADMIN vào được (SecurityConfig: /admin/**). */
@Controller
public class AdminEventController {

	private final EventRepository eventRepo;
	private final SeatRepository seatRepo;
	private final EventService eventService;
	private final ZoneId zone;

	public AdminEventController(EventRepository eventRepo, SeatRepository seatRepo, EventService eventService,
			@Value("${app.zone:Asia/Ho_Chi_Minh}") ZoneId zone) {
		this.eventRepo = eventRepo;
		this.seatRepo = seatRepo;
		this.eventService = eventService;
		this.zone = zone;
	}

	@GetMapping("/admin/events")
	public String list(Model model) {
		Map<Long, SeatStats> stats = seatRepo.statsPerEvent().stream()
				.collect(Collectors.toMap(SeatStats::eventId, Function.identity()));
		model.addAttribute("events", eventRepo.findAll().stream()
				.sorted(Comparator.comparing(Event::getStartTime).reversed()).toList());
		model.addAttribute("stats", stats);
		model.addAttribute("now", Instant.now());
		return "admin-events";
	}

	@GetMapping("/admin/events/new")
	public String form(Model model) {
		model.addAttribute("form", new EventForm());
		return "admin-event-form";
	}

	@PostMapping("/admin/events")
	public String create(@Valid @ModelAttribute("form") EventForm form, BindingResult errors, RedirectAttributes ra) {
		Instant start = form.getStartTime() == null ? null : form.getStartTime().atZone(zone).toInstant();
		if (start != null && !start.isAfter(Instant.now())) {
			errors.rejectValue("startTime", "past", "Thời gian bắt đầu phải ở tương lai");
		}
		if (errors.hasErrors()) {
			return "admin-event-form";
		}
		try {
			Event event = eventService.create(form.getName(), start, form.getRows(), form.getSeatsPerRow());
			ra.addFlashAttribute("success", "Đã tạo sự kiện '" + event.getName() + "' với "
					+ form.getRows() * form.getSeatsPerRow() + " ghế");
			return "redirect:/admin/events";
		}
		catch (DuplicateEventException e) {
			errors.reject("duplicate", e.getMessage());
			return "admin-event-form";
		}
	}

}
