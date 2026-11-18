package com.hien.mvp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				// Ai cũng xem được danh sách sự kiện và sơ đồ ghế
				.requestMatchers(HttpMethod.GET, "/", "/events", "/events/*").permitAll()
				.requestMatchers("/login", "/register", "/app.css", "/error", "/actuator/health").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				// Mọi thứ còn lại (giữ ghế, đặt vé, vé của tôi...) phải đăng nhập
				.anyRequest().authenticated())
			.formLogin(form -> form
				.loginPage("/login")
				.defaultSuccessUrl("/events", false)
				.permitAll())
			.logout(logout -> logout
				.logoutSuccessUrl("/events?logout"));
		// CSRF bật mặc định. Form Thymeleaf dùng th:action nên tự có token.
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
