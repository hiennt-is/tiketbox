package com.hien.mvp.booking;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	String VIEW_SELECT = """
			select new com.hien.mvp.booking.BookingView(
			    b.id, e.id, e.name, e.startTime, s.seatCode, u.username, b.status, b.createdAt, b.cancelledAt)
			from Booking b
			join Seat s on s.id = b.seatId
			join Event e on e.id = s.eventId
			join User u on u.id = b.userId
			""";

	long countBySeatIdAndStatus(Long seatId, BookingStatus status);

	List<Booking> findBySeatIdInAndStatus(Collection<Long> seatIds, BookingStatus status);

	@Query(VIEW_SELECT + " where b.userId = :userId order by e.startTime asc, b.id asc")
	List<BookingView> findViewsByUser(@Param("userId") Long userId);

	@Query(VIEW_SELECT + " order by b.id desc")
	List<BookingView> findLatestViews(Pageable page);

}
