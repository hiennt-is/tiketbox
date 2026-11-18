package com.hien.mvp.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DB lưu thời gian theo UTC. Bean này đổi sang giờ Việt Nam khi hiển thị.
 * Dùng trong template: {@code ${@fmt.dateTime(e.startTime)}}.
 */
@Component("fmt")
public class ViewFormatter {

	private final DateTimeFormatter dateTime;
	private final DateTimeFormatter time;

	public ViewFormatter(@Value("${app.zone:Asia/Ho_Chi_Minh}") ZoneId zone) {
		this.dateTime = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(zone);
		this.time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zone);
	}

	public String dateTime(Instant i) {
		return i == null ? "" : dateTime.format(i);
	}

	public String time(Instant i) {
		return i == null ? "" : time.format(i);
	}

}
