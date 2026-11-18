package com.hien.mvp.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterForm {

	@NotBlank(message = "Không được để trống")
	@Size(min = 3, max = 30, message = "Từ 3 đến 30 ký tự")
	@Pattern(regexp = "[a-zA-Z0-9_.]*", message = "Chỉ gồm chữ không dấu, số, dấu _ và .")
	private String username;

	// BCrypt chỉ dùng 72 byte đầu tiên của mật khẩu
	@Size(min = 8, max = 72, message = "Từ 8 đến 72 ký tự")
	private String password;

	private String confirmPassword;

	@AssertTrue(message = "Mật khẩu nhập lại không khớp")
	public boolean isPasswordConfirmed() {
		return password != null && password.equals(confirmPassword);
	}

}
