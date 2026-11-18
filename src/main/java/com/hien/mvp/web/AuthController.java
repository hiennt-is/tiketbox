package com.hien.mvp.web;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.hien.mvp.user.UserService;
import com.hien.mvp.user.UsernameTakenException;

/** Trang đăng nhập (POST /login do Spring Security tự xử lý) và đăng ký. */
@Controller
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("form", new RegisterForm());
		return "register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult errors) {
		if (errors.hasErrors()) {
			return "register";
		}
		try {
			userService.register(form.getUsername(), form.getPassword());
		}
		catch (UsernameTakenException e) {
			errors.rejectValue("username", "taken", e.getMessage());
			return "register";
		}
		return "redirect:/login?registered";
	}

}
