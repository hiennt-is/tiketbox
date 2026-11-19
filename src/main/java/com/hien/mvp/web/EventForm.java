package com.hien.mvp.web;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.hien.mvp.event.EventService;

import lombok.Data;

@Data
public class EventForm {

	@NotBlank(message = "Không được để trống")
	@Size(min = 3, max = 200, message = "Từ 3 đến 200 ký tự")
	private String name;

	/**
	 * Giờ do admin nhập theo giờ Việt Nam (ô datetime-local không có múi giờ).
	 * Controller đổi sang Instant (UTC) bằng {@code app.zone} trước khi lưu.
	 */
	@NotNull(message = "Hãy chọn thời gian bắt đầu")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime startTime;

	@NotNull(message = "Không được để trống")
	@Min(value = 1, message = "Ít nhất 1 hàng")
	@Max(value = EventService.MAX_ROWS, message = "Tối đa " + EventService.MAX_ROWS + " hàng (A–Z)")
	private Integer rows = 3;

	@NotNull(message = "Không được để trống")
	@Min(value = 1, message = "Ít nhất 1 ghế")
	@Max(value = EventService.MAX_SEATS_PER_ROW, message = "Tối đa " + EventService.MAX_SEATS_PER_ROW + " ghế mỗi hàng")
	private Integer seatsPerRow = 10;

}
