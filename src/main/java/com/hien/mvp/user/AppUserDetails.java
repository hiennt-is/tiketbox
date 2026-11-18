package com.hien.mvp.user;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Người dùng đang đăng nhập, được Spring Security lưu trong session.
 * Có thêm {@code id} để service dùng trực tiếp, không phải tra lại DB theo username.
 * Controller lấy ra bằng {@code @AuthenticationPrincipal AppUserDetails me}.
 */
public record AppUserDetails(Long id, String username, String passwordHash, Role role) implements UserDetails {

	public static AppUserDetails from(User u) {
		return new AppUserDetails(u.getId(), u.getUsername(), u.getPasswordHash(), u.getRole());
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// hasRole("ADMIN") trong SecurityConfig sẽ so với "ROLE_ADMIN"
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	/** Không để lộ hash mật khẩu nếu object bị log ra. */
	@Override
	public String toString() {
		return "AppUserDetails[id=" + id + ", username=" + username + ", role=" + role + "]";
	}

}
