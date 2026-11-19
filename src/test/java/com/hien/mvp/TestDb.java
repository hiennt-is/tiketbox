package com.hien.mvp;

import org.springframework.jdbc.core.JdbcTemplate;

/** Đưa DB test về trạng thái sạch trước mỗi test (giữ lại event, ghế và tài khoản admin). */
public final class TestDb {

	private TestDb() {
	}

	public static void reset(JdbcTemplate jdbc) {
		jdbc.update("DELETE FROM bookings");
		jdbc.update("UPDATE seats SET status = 'AVAILABLE', held_by_user_id = NULL, held_until = NULL");
		// Giữ lại 2 sự kiện seed (id 1, 2), xoá các sự kiện do test tạo ra
		jdbc.update("DELETE FROM seats WHERE event_id > 2");
		jdbc.update("DELETE FROM events WHERE id > 2");
		jdbc.update("DELETE FROM users WHERE username <> 'admin'");
	}

	/** Giả lập "đã hết thời gian giữ" mà không phải chờ 5 phút. */
	public static void expireHold(JdbcTemplate jdbc, Long seatId) {
		jdbc.update("UPDATE seats SET held_until = now() - INTERVAL '1 minute' WHERE id = ?", seatId);
	}

}
