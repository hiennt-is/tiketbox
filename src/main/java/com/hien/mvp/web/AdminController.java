package com.hien.mvp.web;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hien.mvp.booking.BookingException;
import com.hien.mvp.booking.BookingRepository;
import com.hien.mvp.booking.BookingService;
import com.hien.mvp.user.AppUserDetails;

/** Chỉ ADMIN vào được (cấu hình trong SecurityConfig: /admin/**). */
@Controller
public class AdminController {

	private static final int PAGE_SIZE = 200;

	private final BookingRepository bookingRepo;
	private final BookingService bookingService;

	public AdminController(BookingRepository bookingRepo, BookingService bookingService) {
		this.bookingRepo = bookingRepo;
		this.bookingService = bookingService;
	}

	@GetMapping("/admin/bookings")
	public String bookings(Model model) {
		model.addAttribute("bookings", bookingRepo.findLatestViews(PageRequest.of(0, PAGE_SIZE)));
		model.addAttribute("now", Instant.now());
		return "admin-bookings";
	}

	@PostMapping("/admin/bookings/{id}/cancel")
	public String cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
		try {
			bookingService.cancel(id, me.id(), true);
			ra.addFlashAttribute("success", "Đã huỷ vé #" + id);
		}
		catch (BookingException e) {
			ra.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/bookings";
	}

}
