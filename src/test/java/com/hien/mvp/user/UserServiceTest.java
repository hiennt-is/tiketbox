package com.hien.mvp.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hien.mvp.TestDb;
import com.hien.mvp.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserServiceTest {

	@Autowired UserService userService;
	@Autowired UserRepository userRepo;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JdbcTemplate jdbc;

	@BeforeEach
	void setUp() {
		TestDb.reset(jdbc);
	}

	@Test
	void password_is_stored_as_bcrypt_hash() {
		userService.register("carol", "secret-password");

		User carol = userRepo.findByUsername("carol").orElseThrow();
		assertThat(carol.getPasswordHash()).startsWith("$2").doesNotContain("secret-password");
		assertThat(passwordEncoder.matches("secret-password", carol.getPasswordHash())).isTrue();
		assertThat(carol.getRole()).isEqualTo(Role.USER);
	}

	@Test
	void five_people_registering_the_same_username_at_once_only_one_succeeds() throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		List<Future<String>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(5)) {
			for (int i = 0; i < 5; i++) {
				results.add(pool.submit(() -> {
					start.await();
					try {
						userService.register("dave", "password123");
						return "OK";
					}
					catch (UsernameTakenException e) {
						return "TAKEN";
					}
				}));
			}
			start.countDown();
		}

		List<String> outcomes = new ArrayList<>();
		for (Future<String> f : results) {
			outcomes.add(f.get());
		}
		assertThat(outcomes).filteredOn("OK"::equals).hasSize(1);
		assertThat(outcomes).filteredOn("TAKEN"::equals).hasSize(4);
	}

}
