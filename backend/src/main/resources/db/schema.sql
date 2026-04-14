CREATE DATABASE IF NOT EXISTS studyhub DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE studyhub;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role TINYINT NOT NULL DEFAULT 1 COMMENT '1用户 9管理员',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  violation_count INT NOT NULL DEFAULT 0,
  booking_restricted_until DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE user_profile (
  user_id BIGINT PRIMARY KEY,
  nickname VARCHAR(50) NOT NULL,
  avatar_url VARCHAR(255) NULL,
  preferred_room_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE study_room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  location_desc VARCHAR(255) NOT NULL,
  facilities VARCHAR(255) NULL COMMENT '打印,饮水,储物柜',
  seat_total INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1开放 0关闭',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE room_open_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  day_type TINYINT NOT NULL COMMENT '1工作日 2节假日',
  open_time TIME NOT NULL,
  close_time TIME NOT NULL,
  CONSTRAINT fk_open_rule_room FOREIGN KEY (room_id) REFERENCES study_room(id),
  UNIQUE KEY uk_room_day_type (room_id, day_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE seat (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  seat_no VARCHAR(20) NOT NULL,
  seat_type TINYINT NOT NULL COMMENT '1普通 2静音 3插座',
  x_pos INT NOT NULL DEFAULT 0,
  y_pos INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1可用 2维修关闭',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_seat_room FOREIGN KEY (room_id) REFERENCES study_room(id),
  UNIQUE KEY uk_room_seat_no (room_id, seat_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE booking_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL UNIQUE,
  max_bookings_per_day INT NOT NULL DEFAULT 2,
  max_hours_per_booking INT NOT NULL DEFAULT 4,
  checkin_grace_minutes INT NOT NULL DEFAULT 120,
  temp_leave_minutes INT NOT NULL DEFAULT 15,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_booking_rule_room FOREIGN KEY (room_id) REFERENCES study_room(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE seat_slot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  seat_id BIGINT NOT NULL,
  booking_date DATE NOT NULL,
  slot_code TINYINT NOT NULL COMMENT '1早 2中 3晚',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0空闲 1已预订 2临时离开 3关闭',
  current_order_id BIGINT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_slot_seat FOREIGN KEY (seat_id) REFERENCES seat(id),
  UNIQUE KEY uk_seat_date_slot (seat_id, booking_date, slot_code),
  KEY idx_slot_status (booking_date, slot_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE booking_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  seat_id BIGINT NOT NULL,
  booking_date DATE NOT NULL,
  slot_code TINYINT NOT NULL COMMENT '1早 2中 3晚',
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待签到 1使用中 2已完成 3已取消 4爽约',
  voucher_code VARCHAR(80) NOT NULL UNIQUE,
  cancel_reason VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_order_room FOREIGN KEY (room_id) REFERENCES study_room(id),
  CONSTRAINT fk_order_seat FOREIGN KEY (seat_id) REFERENCES seat(id),
  KEY idx_order_user_date (user_id, booking_date),
  KEY idx_order_room_date_slot (room_id, booking_date, slot_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE checkin_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  checkin_time DATETIME NULL,
  checkout_time DATETIME NULL,
  checkin_method TINYINT NOT NULL DEFAULT 1 COMMENT '1扫码',
  checkout_method TINYINT NOT NULL DEFAULT 1 COMMENT '1扫码',
  CONSTRAINT fk_checkin_order FOREIGN KEY (order_id) REFERENCES booking_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE temp_leave_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  leave_start DATETIME NOT NULL,
  expected_back_time DATETIME NOT NULL,
  actual_back_time DATETIME NULL,
  is_timeout TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_leave_order FOREIGN KEY (order_id) REFERENCES booking_order(id),
  KEY idx_leave_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE violation_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_id BIGINT NULL,
  violation_type TINYINT NOT NULL COMMENT '1迟到/未签到 2占座 3临时离开超时',
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_violation_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  KEY idx_violation_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE violation_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_user_id BIGINT NOT NULL,
  order_id BIGINT NULL,
  seat_id BIGINT NOT NULL,
  report_type TINYINT NOT NULL DEFAULT 2 COMMENT '1迟到/未签到 2占座 3临时离开超时',
  image_url VARCHAR(255) NULL,
  description_text VARCHAR(255) NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1属实 2驳回',
  handle_admin_id BIGINT NULL,
  handle_note VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handled_at DATETIME NULL,
  CONSTRAINT fk_reporter_user FOREIGN KEY (reporter_user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_report_seat FOREIGN KEY (seat_id) REFERENCES seat(id),
  KEY idx_report_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  order_id BIGINT NULL,
  score TINYINT NOT NULL COMMENT '1~5',
  content VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_review_room FOREIGN KEY (room_id) REFERENCES study_room(id),
  KEY idx_review_room (room_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  msg_type TINYINT NOT NULL COMMENT '1预约成功 2取消 3签到提醒 4离开超时 5维修完成 6优惠券到账',
  title VARCHAR(100) NOT NULL,
  content VARCHAR(500) NOT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notice_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  KEY idx_notice_user_read (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE points_account (
  user_id BIGINT PRIMARY KEY,
  points_balance INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_points_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE points_txn (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  change_amount INT NOT NULL,
  biz_type TINYINT NOT NULL COMMENT '1按时签退奖励 2兑换扣减 3管理员调整 4违规扣减',
  biz_ref_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_points_txn_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  KEY idx_points_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE coupon_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  points_cost INT NOT NULL DEFAULT 0,
  valid_days INT NOT NULL DEFAULT 30,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE user_coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
  obtained_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_at DATETIME NULL,
  expire_at DATETIME NOT NULL,
  CONSTRAINT fk_uc_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_uc_tpl FOREIGN KEY (template_id) REFERENCES coupon_template(id),
  KEY idx_uc_user_status (user_id, status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE stats_daily (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_date DATE NOT NULL,
  room_id BIGINT NOT NULL,
  total_slots INT NOT NULL DEFAULT 0,
  booked_slots INT NOT NULL DEFAULT 0,
  usage_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  monthly_booking_count INT NOT NULL DEFAULT 0,
  repeat_user_count INT NOT NULL DEFAULT 0,
  violation_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_stats_room FOREIGN KEY (room_id) REFERENCES study_room(id),
  UNIQUE KEY uk_stat_date_room (stat_date, room_id)
) ENGINE=InnoDB;
