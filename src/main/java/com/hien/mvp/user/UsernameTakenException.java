package com.hien.mvp.user;

public class UsernameTakenException extends RuntimeException {

	public UsernameTakenException(String username) {
		super("Tên đăng nhập '" + username + "' đã có người dùng");
	}

}
