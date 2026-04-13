USE studyhub;

-- password: 123456 (sha256)
INSERT INTO sys_user(id, phone, password_hash, role, status, violation_count)
VALUES
  (1, '13800000001', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 9, 1, 0),
  (2, '13800000002', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, 1, 0)
ON DUPLICATE KEY UPDATE phone = VALUES(phone);

INSERT INTO user_profile(user_id, nickname, preferred_room_id)
VALUES (1, '管理员', NULL), (2, '测试用户', NULL)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO points_account(user_id, points_balance)
VALUES (1, 0), (2, 100)
ON DUPLICATE KEY UPDATE points_balance = VALUES(points_balance);

INSERT INTO points_txn(user_id, change_amount, biz_type, biz_ref_id)
SELECT 2, 100, 3, NULL
WHERE NOT EXISTS (
  SELECT 1 FROM points_txn WHERE user_id = 2
);

INSERT INTO study_room(id, name, location_desc, facilities, seat_total, status)
VALUES
  (1, '主馆一层自习室', '图书馆一层东侧', '打印,饮水,储物柜', 24, 1),
  (2, '主馆二层静音区', '图书馆二层北侧', '饮水,储物柜', 18, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), location_desc = VALUES(location_desc);

INSERT INTO room_open_rule(room_id, day_type, open_time, close_time)
VALUES
  (1, 1, '08:00:00', '22:30:00'),
  (1, 2, '09:00:00', '21:30:00'),
  (2, 1, '08:30:00', '22:00:00'),
  (2, 2, '09:00:00', '21:00:00')
ON DUPLICATE KEY UPDATE open_time = VALUES(open_time), close_time = VALUES(close_time);

INSERT INTO booking_rule(room_id, max_bookings_per_day, max_hours_per_booking, checkin_grace_minutes, temp_leave_minutes)
VALUES
  (1, 2, 4, 120, 15),
  (2, 2, 4, 120, 15)
ON DUPLICATE KEY UPDATE max_bookings_per_day = VALUES(max_bookings_per_day);

INSERT INTO coupon_template(id, name, points_cost, valid_days, status)
VALUES
  (1, '9折学习券', 80, 30, 1),
  (2, '免费打印券', 120, 30, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- room 1 seats
INSERT INTO seat(room_id, seat_no, seat_type, x_pos, y_pos, status)
VALUES
  (1, 'A01', 1, 1, 1, 1), (1, 'A02', 1, 2, 1, 1), (1, 'A03', 1, 3, 1, 1), (1, 'A04', 1, 4, 1, 1),
  (1, 'B01', 2, 1, 2, 1), (1, 'B02', 2, 2, 2, 1), (1, 'B03', 2, 3, 2, 1), (1, 'B04', 2, 4, 2, 1),
  (1, 'C01', 3, 1, 3, 1), (1, 'C02', 3, 2, 3, 1), (1, 'C03', 3, 3, 3, 1), (1, 'C04', 3, 4, 3, 1)
ON DUPLICATE KEY UPDATE seat_type = VALUES(seat_type), status = VALUES(status);

-- room 2 seats
INSERT INTO seat(room_id, seat_no, seat_type, x_pos, y_pos, status)
VALUES
  (2, 'S01', 2, 1, 1, 1), (2, 'S02', 2, 2, 1, 1), (2, 'S03', 2, 3, 1, 1), (2, 'S04', 2, 4, 1, 1),
  (2, 'P01', 3, 1, 2, 1), (2, 'P02', 3, 2, 2, 1), (2, 'P03', 3, 3, 2, 1), (2, 'P04', 3, 4, 2, 1)
ON DUPLICATE KEY UPDATE seat_type = VALUES(seat_type), status = VALUES(status);
