package com.hien.mvp.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.hien.mvp.user.AppUserDetails;

/** Đưa người dùng đang đăng nhập vào mọi template dưới tên {@code me} (null nếu chưa đăng nhập). */
@ControllerAdvice
public class CurrentUserAdvice {

	@ModelAttribute("me")
	public AppUserDetails me(@AuthenticationPrincipal AppUserDetails me) {
		return me;
	}

}
