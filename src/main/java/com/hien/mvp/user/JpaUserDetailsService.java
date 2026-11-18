package com.hien.mvp.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Spring Security gọi class này khi người dùng submit form đăng nhập. */
@Service
public class JpaUserDetailsService implements UserDetailsService {

	private final UserRepository userRepo;

	public JpaUserDetailsService(UserRepository userRepo) {
		this.userRepo = userRepo;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		return userRepo.findByUsername(username)
				.map(AppUserDetails::from)
				.orElseThrow(() -> new UsernameNotFoundException(username));
	}

}
