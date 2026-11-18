INSERT INTO events (name, start_time)
VALUES ('Concert Mùa Đông', now() + INTERVAL '7 days'),
       ('Phim: Lập trình viên cuối cùng', now() + INTERVAL '3 days');

-- Mỗi event 30 ghế: hàng A, B, C x số 1..10
INSERT INTO seats (event_id, seat_code)
SELECT e.id, r.row_letter || n.num
FROM events e
         CROSS JOIN (VALUES ('A', 1), ('B', 2), ('C', 3)) AS r(row_letter, row_order)
         CROSS JOIN generate_series(1, 10) AS n(num)
ORDER BY e.id, r.row_order, n.num;
