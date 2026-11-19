# TicketBox: đặt vé sự kiện (MVP)

Web đặt vé sự kiện có đăng nhập. Xử lý tranh chấp ghế bằng **optimistic lock**.
Stack: Spring Boot 4 · Spring Security · JPA/Hibernate · PostgreSQL 17 · Flyway · Thymeleaf · Testcontainers.

## Chạy

```bash
docker compose up -d          # Postgres, đọc cấu hình từ .env
./mvnw spring-boot:run        # http://localhost:8080
./mvnw test                   # cần Docker đang chạy (Testcontainers)
```

`.env` được dùng chung cho docker compose và Spring Boot (`spring.config.import`).
Đặt `ADMIN_PASSWORD` trong `.env` thì lúc khởi động app sẽ tự tạo tài khoản `admin`.

Deploy miễn phí lên Render + Neon: xem [DEPLOY.md](DEPLOY.md).

## Chức năng

| Ai | Làm được gì |
|---|---|
| Khách | Xem sự kiện, xem sơ đồ ghế, đăng ký, đăng nhập |
| Người dùng | Bấm ghế để **giữ 5 phút** (tối đa 4 ghế), bấm lại để bỏ giữ, **xác nhận** đặt tất cả ghế đang giữ, xem **Vé của tôi**, **huỷ vé** trước giờ diễn |
| Admin | **Tạo sự kiện** (tự sinh sơ đồ ghế theo số hàng × số ghế mỗi hàng), xem số ghế đã bán, xem toàn bộ booking, huỷ bất kỳ vé nào |

Luồng chính:

```
AVAILABLE --giữ--> HELD --xác nhận--> BOOKED
    ^               |                   |
    +--bỏ giữ/hết hạn                   |
    +--------------huỷ vé---------------+
```

## Xử lý race condition

| Tình huống | Cách chặn |
|---|---|
| 2 người cùng giữ 1 ghế | `@Version` trên `Seat`: `UPDATE ... WHERE version=?`, người ghi sau được 0 dòng nên bị rollback |
| Bấm "Xác nhận" nhiều lần | Lần đầu chuyển ghế sang BOOKED; các lần sau không còn ghế HELD, hoặc thua version |
| Xác nhận đúng lúc job nhả ghế hết hạn | Job tăng `version`, nên lần xác nhận đó thua và **không ghế nào** được đặt |
| Đặt nhiều ghế | 1 transaction: được hết hoặc không được ghế nào |
| 2 người đăng ký trùng username | Ràng buộc `UNIQUE`, không kiểm tra trước rồi mới insert |
| Admin bấm "Tạo sự kiện" 2 lần | `UNIQUE (name, start_time)`; sự kiện và toàn bộ ghế lưu trong 1 transaction |
| Mọi bug còn sót | Unique index `ux_booking_seat_confirmed`: mỗi ghế tối đa 1 booking CONFIRMED |

Chi tiết nằm ở [BookingTransactions.java](src/main/java/com/hien/mvp/booking/BookingTransactions.java).
[BookingService.java](src/main/java/com/hien/mvp/booking/BookingService.java) đứng ngoài transaction để bắt lỗi
xảy ra lúc commit và đổi thành thông báo cho người dùng.

## Bảo mật

- Form login + session; mật khẩu hash bằng BCrypt; bật CSRF cho mọi form.
- Danh tính lấy từ session (`@AuthenticationPrincipal`), không lấy từ form.
- Huỷ vé của người khác: báo "không tìm thấy", không để lộ là vé đó có tồn tại.
- `/admin/**` chỉ dành cho `ROLE_ADMIN`.

## Cấu trúc

```
user/      User, AppUserDetails, UserService (đăng ký), AdminInitializer
security/  SecurityConfig
event/     Event, EventService (tạo sự kiện + sinh ghế)
seat/      Seat (@Version), SeatRepository
booking/   BookingTransactions (transaction), BookingService (dịch lỗi), HoldExpiryJob
web/       Controller, SeatView, ViewFormatter (giờ VN), CurrentUserAdvice
```

## Giới hạn đã biết

- Giới hạn 4 ghế được đếm trước khi ghi, nên 2 request giữ 2 ghế **khác nhau** của cùng một người chạy
  song song có thể giữ được 5 ghế. Chấp nhận được với app nhỏ (đã ghi chú trong code).
- Chưa có thanh toán, email, quản lý sự kiện qua giao diện.
