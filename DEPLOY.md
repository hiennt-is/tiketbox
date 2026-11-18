# Deploy miễn phí: Render (app) + Neon (PostgreSQL)

Cần chuẩn bị: tài khoản GitHub, [neon.tech](https://neon.tech) và [render.com](https://render.com). Có thể đăng nhập cả hai bằng GitHub.
Tổng thời gian khoảng 20 phút, phần lớn là chờ Render build lần đầu.

---

## Bước 1: Đưa code lên GitHub

1. Vào GitHub → **New repository** → đặt tên, ví dụ `ticketbox`. Chọn Private hay Public đều được.
   **Không** tick "Add README" để repo trống hoàn toàn.
2. Ở thư mục project, chạy:

```bash
git remote add origin https://github.com/<tên-github-của-bạn>/ticketbox.git
git push -u origin main
```

> File `.env` (chứa mật khẩu) đã nằm trong `.gitignore` nên **không** bị đẩy lên GitHub.

## Bước 2: Tạo database trên Neon

1. Neon → **New Project** → Region chọn **AWS Asia Pacific (Singapore)**, Postgres version **17**.
2. Mở **Connect** (hoặc Dashboard → Connection Details):
   - **Tắt** "Connection pooling", tức là dùng kết nối trực tiếp (host **không** có chữ `-pooler`).
     Flyway và Hibernate chạy ổn định nhất với kết nối trực tiếp.
   - Neon hiện chuỗi dạng:
     `postgresql://neondb_owner:AbC123xyz@ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`
3. Tách chuỗi đó thành **3 giá trị** cho Spring Boot:

| Biến | Giá trị (theo ví dụ trên) |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
| `SPRING_DATASOURCE_PASSWORD` | `AbC123xyz` |

> Lưu ý: URL phải bắt đầu bằng `jdbc:postgresql://`, **không** chứa user:password, và **giữ** `?sslmode=require`.

Không cần tạo bảng: lần chạy đầu tiên, Flyway tự tạo bảng và dữ liệu mẫu.

## Bước 3: Tạo service trên Render

1. Render → **New +** → **Blueprint** → kết nối GitHub → chọn repo `ticketbox`.
2. Render đọc `render.yaml` rồi hỏi các biến có `sync: false`. Điền:
   - 3 biến database ở Bước 2
   - `ADMIN_PASSWORD`: mật khẩu cho tài khoản `admin`. Hãy đặt mật khẩu **mạnh**, vì đây là web public.
3. Bấm **Apply**. Lần build đầu mất khoảng 5–10 phút. Bạn theo dõi ở tab **Logs**, đợi dòng:
   ```
   Started MvpApplication in ... seconds
   Đã tạo tài khoản admin 'admin'
   ```
4. Mở URL Render cấp, dạng `https://ticketbox-xxxx.onrender.com`:
   - `/actuator/health` phải trả về `{"status":"UP"}`
   - Đăng nhập `admin` / mật khẩu vừa đặt, rồi vào **Quản trị**.

## Cập nhật sau này

Mỗi lần `git push` lên `main`, Render tự build và deploy lại. Migration mới (V5, V6...) được Flyway tự chạy.

---

## Cần biết về gói miễn phí

- **App ngủ** sau khoảng 15 phút không có ai truy cập. Lần mở tiếp theo phải chờ khoảng 30–60 giây.
  Khi app ngủ, job nhả ghế không chạy, nhưng không ảnh hưởng: ghế hết hạn giữ vẫn được coi là trống.
- Session đăng nhập nằm trong bộ nhớ, nên **mỗi lần app ngủ dậy hoặc deploy lại, người dùng phải đăng nhập lại**.
- Neon free tự tạm dừng database khi rảnh. Kết nối đầu tiên chậm hơn khoảng 1 giây, không cần làm gì.
- Giới hạn và điều khoản của gói free có thể thay đổi, nên xem lại trang giá của Render và Neon.

## Thêm sự kiện mới

Dữ liệu mẫu có 2 sự kiện, diễn ra 3 và 7 ngày **kể từ lúc deploy lần đầu**. Để thêm sự kiện, mở
**Neon → SQL Editor** và chạy:

```sql
WITH e AS (
  INSERT INTO events (name, start_time)
  VALUES ('Tên sự kiện mới', '2027-01-15 20:00:00+07')
  RETURNING id
)
INSERT INTO seats (event_id, seat_code)
SELECT e.id, r.l || n.n
FROM e, (VALUES ('A'), ('B'), ('C')) r(l), generate_series(1, 10) n(n)
ORDER BY r.l, n.n;
```

## Lỗi thường gặp

| Log trên Render | Cách sửa |
|---|---|
| `password authentication failed` | Sai username/password, hoặc URL có chứa `user:pass@`: hãy tách đúng như Bước 2 |
| `The server does not support SSL` / `SSL required` | Kiểm tra `?sslmode=require` ở cuối URL |
| `Connection refused` tới `localhost:5432` | Chưa đặt `SPRING_DATASOURCE_URL` (app đang dùng giá trị mặc định cho local) |
| `prepared statement "S_1" already exists` | Đang dùng host `-pooler`: đổi sang kết nối trực tiếp |
| `Out of memory` / bị restart liên tục | Kiểm tra `JAVA_OPTS` trong Dockerfile vẫn có `-XX:MaxRAMPercentage=70` |
| Không có tài khoản admin | Chưa đặt `ADMIN_PASSWORD`. Đặt biến này rồi bấm **Manual Deploy** |
