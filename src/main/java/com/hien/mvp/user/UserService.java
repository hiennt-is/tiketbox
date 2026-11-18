package com.hien.mvp.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
		this.userRepo = userRepo;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Đăng ký tài khoản.
	 *
	 * <p>KHÔNG kiểm tra trước bằng {@code existsByUsername} rồi mới insert: đó lại là lỗi
	 * check-then-act, vì 2 người đăng ký cùng lúc đều thấy "chưa tồn tại". Thay vào đó cứ INSERT
	 * và để ràng buộc UNIQUE của DB quyết định ai thắng.
	 *
	 * <p>Method này cố ý không có {@code @Transactional}: {@code saveAndFlush} tự chạy trong
	 * transaction của repository, nên lỗi unique được ném ra ngay tại đây và bắt được.
	 */
	public User register(String username, String rawPassword) {
		return create(username, rawPassword, Role.USER);
	}

	User create(String username, String rawPassword, Role role) {
		try {
			return userRepo.saveAndFlush(new User(username.trim(), passwordEncoder.encode(rawPassword), role));
		}
		catch (DataIntegrityViolationException e) {
			throw new UsernameTakenException(username);
		}
	}

}
