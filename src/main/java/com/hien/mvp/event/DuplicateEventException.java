package com.hien.mvp.event;

public class DuplicateEventException extends RuntimeException {

	public DuplicateEventException(String name) {
		super("Đã có sự kiện '" + name + "' vào đúng thời gian này");
	}

}
