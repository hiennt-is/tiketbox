package com.hien.mvp.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Tạo tài khoản admin đầu tiên lúc khởi động, nếu chưa có.
 * Mật khẩu lấy từ cấu hình ({@code ADMIN_PASSWORD}), không ghi vào migration hay source code.
 */
@Component
class AdminInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

	private final UserRepository userRepo;
	private final UserService userService;
	private final String username;
	private final String password;

	AdminInitializer(UserRepository userRepo, UserService userService,
			@Value("${app.admin.username:admin}") String username,
			@Value("${app.admin.password:}") String password) {
		this.userRepo = userRepo;
		this.userService = userService;
		this.username = username;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (userRepo.existsByRole(Role.ADMIN)) {
			return;
		}
		if (password.isBlank()) {
			log.warn("Chưa có tài khoản admin. Đặt ADMIN_PASSWORD để tự tạo lúc khởi động.");
			return;
		}
		try {
			userService.create(username, password, Role.ADMIN);
			log.info("Đã tạo tài khoản admin '{}'", username);
		}
		catch (UsernameTakenException e) {
			log.warn("Không tạo được admin: username '{}' đã được dùng bởi tài khoản thường", username);
		}
	}

}
