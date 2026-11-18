package com.hien.mvp.web;

import java.time.Instant;

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

@Controller
public class MyBookingsController {

	private final BookingRepository bookingRepo;
	private final BookingService bookingService;

	public MyBookingsController(BookingRepository bookingRepo, BookingService bookingService) {
		this.bookingRepo = bookingRepo;
		this.bookingService = bookingService;
	}

	@GetMapping("/me/bookings")
	public String myBookings(@AuthenticationPrincipal AppUserDetails me, Model model) {
		model.addAttribute("bookings", bookingRepo.findViewsByUser(me.id()));
		model.addAttribute("now", Instant.now());
		return "my-bookings";
	}

	@PostMapping("/me/bookings/{id}/cancel")
	public String cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
		try {
			// admin = false: kể cả admin, ở trang này cũng chỉ huỷ được vé của chính mình
			bookingService.cancel(id, me.id(), false);
			ra.addFlashAttribute("success", "Đã huỷ vé #" + id);
		}
		catch (BookingException e) {
			ra.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/me/bookings";
	}

}
