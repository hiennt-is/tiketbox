package com.hien.mvp.seat;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	List<Seat> findByEventIdOrderByIdAsc(Long eventId);

	/** Các ghế {@code userId} đang giữ trong 1 sự kiện (kể cả đã hết hạn nhưng job chưa kịp nhả). */
	List<Seat> findByEventIdAndStatusAndHeldByUserIdOrderByIdAsc(Long eventId, SeatStatus status, Long userId);

	@Query("""
			select count(s) from Seat s
			where s.eventId = :eventId and s.status = com.hien.mvp.seat.SeatStatus.HELD
			  and s.heldByUserId = :userId and s.heldUntil >= :now""")
	long countActiveHolds(@Param("eventId") Long eventId, @Param("userId") Long userId, @Param("now") Instant now);

	/**
	 * Nhả các ghế giữ quá hạn bằng 1 câu UPDATE.
	 * Có tăng {@code version}: nếu đúng lúc đó người dùng đang bấm "Xác nhận", transaction của họ
	 * sẽ bị lỗi optimistic lock thay vì ghi đè lên trạng thái vừa bị nhả.
	 */
	@Modifying
	@Query("""
			update Seat s
			set s.status = com.hien.mvp.seat.SeatStatus.AVAILABLE, s.heldByUserId = null, s.heldUntil = null,
			    s.version = s.version + 1
			where s.status = com.hien.mvp.seat.SeatStatus.HELD and s.heldUntil < :now""")
	int releaseExpiredHolds(@Param("now") Instant now);

}
