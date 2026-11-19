-- Chặn tạo trùng sự kiện (cùng tên + cùng giờ bắt đầu), ví dụ khi admin bấm "Tạo" 2 lần.
-- Giống đăng ký username: không kiểm tra trước rồi mới insert, mà để ràng buộc UNIQUE quyết định.
ALTER TABLE events ADD CONSTRAINT uq_event_name_start UNIQUE (name, start_time);
